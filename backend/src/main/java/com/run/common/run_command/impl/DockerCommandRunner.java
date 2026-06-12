package com.run.common.run_command.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.run.common.run_command.CommandResult;
import com.run.common.run_command.CommandRunner;
import com.run.common.run_command.CommandRunnerLifecycle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Docker 容器运行时。
 * driver 池：平台线程，大小 = 并发上限 = max(1, cores/2)；有界队列排队，满则拒绝。
 * 容器重（每个一份内存），并发上限收紧，避免拖垮服务器。读流走共享虚拟线程池 STREAM_EXECUTOR。
 * hush-toolbox 已退化为纯脚本执行器：不走 Hush 代理，因此不挂载 CA 证书、不注入 HTTP(S)_PROXY。
 *
 * <p><b>exactly-once 终态保证（结构性）</b>：实例级 {@code terminated} 守卫 + {@code catch (Throwable)}
 * + finally 终极兜底，保证每次执行恰好一次 onComplete / onError。
 *
 * <p><b>取消语义</b>：kill() 停容器，follow 日志流随之关闭；终态直接取累积内容，读流仅做合并短兜底
 * 排空。{@code awaitStatusCode()} 已加超时包裹，避免停容器竞态下无限阻塞。onNext 经守卫，不会晚于终态。
 *
 * <p>本类实例为单次使用，每次执行新建一个实例。
 */
public final class DockerCommandRunner implements CommandRunner {

    private static final String DOCKER_IMAGE = "ghcr.io/runify-dev/hush-toolbox:v0.1.0";
    /**
     * 拉镜像用独立超时，不占用命令执行的时间预算
     */
    private static final int IMAGE_PULL_TIMEOUT = 600;

    /**
     * 取消/退出后读流的合并兜底排空时长（秒），也用作 stopContainer 优雅停止超时与状态码获取超时。
     */
    private static final int STREAM_DRAIN = CommandRunner.resolveQueueCap("runify.terminal.drain.seconds", 2);

    private static final int CORES = Runtime.getRuntime().availableProcessors();

    /**
     * docker driver 池：core == max = cores/2，超出进有界队列排队，队列满 → AbortPolicy 抛异常
     */
    private static final ThreadPoolExecutor DRIVER_EXECUTOR = new ThreadPoolExecutor(
            Math.max(1, CORES / 2), Math.max(1, CORES / 2),
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(CommandRunner.resolveQueueCap("runify.terminal.docker.queue", 200)),
            CommandRunner.namedFactory("runify-docker"),
            new ThreadPoolExecutor.AbortPolicy());

    static {
        // 登记本池，供优雅关停统一管理
        CommandRunnerLifecycle.registerPool(DRIVER_EXECUTOR);
    }

    private final String command;
    private final Map<String, String> env;
    private final String workDir;
    private final int timeout;

    private final StringBuilder stdoutSb = new StringBuilder();
    private final StringBuilder stderrSb = new StringBuilder();

    private volatile DockerClient client;
    private volatile String containerId;
    /**
     * 标记是否被外部 kill；用于区分“超时”与“取消”，并支持 kill 抢跑（容器尚未启动即被取消）。
     */
    private volatile boolean killed = false;
    /**
     * exactly-once 终态守卫。
     */
    private final AtomicBoolean terminated = new AtomicBoolean(false);
    /**
     * 单次使用守卫：run() 不可重复调用。
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    public DockerCommandRunner(String command, Map<String, String> env, String workDir, int timeout) {
        this.command = command;
        this.env = env;
        this.workDir = workDir;
        this.timeout = timeout;
    }

    @Override
    public void run(Listener listener) {
        // 单次使用：重复 run() 直接拒绝
        if (!started.compareAndSet(false, true)) {
            listener.onError(new IllegalStateException("DockerCommandRunner 为单次使用，run() 不可重复调用"));
            return;
        }

        // onNext 守卫：终态之后丢弃迟到分片
        Consumer<String> emit = chunk -> {
            if (!terminated.get()) {
                listener.onNext(chunk);
            }
        };

        Runnable driver = () -> {
            DockerClient dc = null;
            String cid = null;
            CompletableFuture<Void> stdoutFuture = null;
            CompletableFuture<Void> stderrFuture = null;
            try {
                dc = DockerClientBuilder.getInstance().build();
                this.client = dc;
                final DockerClient client = dc;

                // 拉镜像用独立超时，不占用命令执行的时间预算
                client.pullImageCmd(DOCKER_IMAGE)
                        .exec(new PullImageResultCallback())
                        .awaitCompletion(IMAGE_PULL_TIMEOUT, TimeUnit.SECONDS);

                List<String> envList = new ArrayList<>();
                if (env != null) {
                    env.forEach((k, v) -> envList.add(k + "=" + v));
                }

                // 去掉了 withAutoRemove(true)：与读退出码 inspect 形成竞态，改为 finally 手动删除。
                CreateContainerResponse container = client.createContainerCmd(DOCKER_IMAGE)
                        .withHostConfig(HostConfig.newHostConfig()
                                .withBinds(Bind.parse(workDir + ":/workspace")))
                        .withEnv(envList)
                        .withWorkingDir("/workspace")
                        .withCmd("sh", "-c", command)
                        .exec();

                cid = container.getId();
                this.containerId = cid;
                final String containerId = cid;

                // 先注册等待退出回调，再 start；wait 端点阻塞到容器 stopped，可靠捕获秒级退出码
                WaitContainerResultCallback waitCallback =
                        client.waitContainerCmd(cid).exec(new WaitContainerResultCallback());

                stdoutFuture = followLog(client, containerId, true, emit);
                stderrFuture = followLog(client, containerId, false, null);

                client.startContainerCmd(cid).exec();

                // kill 抢跑保护：拉镜像/创建期间被取消，start 之后立即停容器
                if (killed) {
                    stopContainer(client, containerId);
                }

                // kill() 会停容器，awaitCompletion 随即返回；因此取消由 kill 触发、走 onComplete
                boolean finished = waitCallback.awaitCompletion(timeout, TimeUnit.SECONDS);
                if (!finished && !killed) {
                    // 真·超时（非取消）
                    stopContainer(client, cid);
                    fail(listener, new RuntimeException("命令执行超时（" + timeout + "秒）"));
                    return;
                }

                // awaitStatusCode() 无超时：停容器竞态下可能无限阻塞，必须加超时包裹
                int exitCode = awaitExitCodeBounded(waitCallback);

                // 合并短兜底排空读流；完不完成都不阻塞终态，直接取累积内容
                drainStreams(stdoutFuture, stderrFuture);
                String stdout, stderr;
                synchronized (stdoutSb) {
                    stdout = stdoutSb.toString();
                }
                synchronized (stderrSb) {
                    stderr = stderrSb.toString();
                }
                complete(listener, new CommandResult(exitCode, stdout, stderr));
            } catch (Throwable t) {
                // Throwable：连 Error 一起兜住，避免 driver 线程静默死亡而不回调
                if (dc != null && cid != null) {
                    stopContainer(dc, cid);
                }
                fail(listener, t);
            } finally {
                if (stdoutFuture != null) stdoutFuture.exceptionally(ex -> null);
                if (stderrFuture != null) stderrFuture.exceptionally(ex -> null);
                // 手动删除容器（替代 autoRemove），覆盖正常/取消/超时/异常所有路径
                if (dc != null && cid != null) {
                    removeContainer(dc, cid);
                }
                if (dc != null) {
                    try {
                        dc.close();
                    } catch (Exception ignored) {
                    }
                }
                // 终极兜底：任何路径都没产生终态 → 兜一个 onError
                if (terminated.compareAndSet(false, true)) {
                    listener.onError(new IllegalStateException("命令 driver 结束但未产生终态回调（结构性兜底触发）"));
                }
                CommandRunnerLifecycle.untrack(this);
            }
        };

        // 提交前登记；关停进行中则直接拒绝
        if (!CommandRunnerLifecycle.track(this)) {
            fail(listener, new RuntimeException("服务正在关停，已拒绝新的 docker 命令"));
            return;
        }
        try {
            DRIVER_EXECUTOR.execute(driver);
        } catch (RejectedExecutionException e) {
            // 队列满 / 池已关闭：在调用线程上立即拒绝 → 走失败
            CommandRunnerLifecycle.untrack(this);
            fail(listener, new RuntimeException("docker 执行队列已满，请稍后重试", e));
        } catch (Throwable t) {
            // 提交阶段的任何其它异常：driver 未启动、finally 兜底不存在，这里必须补一个终态
            CommandRunnerLifecycle.untrack(this);
            fail(listener, t);
        }
    }

    private CompletableFuture<Void> followLog(DockerClient dc, String cid, boolean stdout, Consumer<String> onChunk) {
        StringBuilder sink = stdout ? stdoutSb : stderrSb;
        return CompletableFuture.runAsync(() -> {
            try {
                dc.logContainerCmd(cid)
                        .withStdOut(stdout).withStdErr(!stdout).withFollowStream(true)
                        .exec(new LogContainerResultCallback() {
                            @Override
                            public void onNext(Frame frame) {
                                String chunk = new String(frame.getPayload(), StandardCharsets.UTF_8);
                                synchronized (sink) {
                                    sink.append(chunk);
                                }
                                if (onChunk != null) {
                                    onChunk.accept(chunk);
                                }
                            }
                        }).awaitCompletion(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                // 容器被停/删导致 follow 流关闭，属预期；已读内容已在 sink 中
            }
        }, STREAM_EXECUTOR);
    }

    @Override
    public void kill() {
        killed = true;
        String cid = this.containerId;
        DockerClient dc = this.client;
        if (cid != null && dc != null) {
            stopContainer(dc, cid);
        }
    }

    // ---- 终态把关（exactly-once）----

    private void complete(Listener l, CommandResult r) {
        if (terminated.compareAndSet(false, true)) {
            l.onComplete(r);
        }
    }

    private void fail(Listener l, Throwable e) {
        if (terminated.compareAndSet(false, true)) {
            l.onError(e);
        }
    }

    // ---- docker 专属底层工具 ----

    /**
     * 给无超时的 awaitStatusCode() 套一个超时；拿不到状态码（取消竞态）则返回 -1。
     */
    private int awaitExitCodeBounded(WaitContainerResultCallback waitCallback) {
        try {
            return CompletableFuture.supplyAsync(waitCallback::awaitStatusCode, STREAM_EXECUTOR)
                    .get(STREAM_DRAIN, TimeUnit.SECONDS);
        } catch (Exception e) {
            return -1;
        }
    }

    private static void drainStreams(CompletableFuture<?>... futures) {
        try {
            CompletableFuture.allOf(futures).get(STREAM_DRAIN, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private static void stopContainer(DockerClient dc, String cid) {
        try {
            dc.stopContainerCmd(cid).withTimeout(STREAM_DRAIN).exec();
        } catch (Exception ignored) {
        }
    }

    private static void removeContainer(DockerClient dc, String cid) {
        try {
            dc.removeContainerCmd(cid).withForce(true).exec();
        } catch (Exception ignored) {
        }
    }
}