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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Docker 容器运行时。
 * driver 池：平台线程，大小 = 并发上限 = max(1, cores/2)；有界队列排队，满则拒绝。
 * 容器重（每个一份内存），并发上限收紧，避免拖垮服务器。读流走共享虚拟线程池 STREAM_EXECUTOR。
 * hush-toolbox 已退化为纯脚本执行器：不走 Hush 代理，因此不挂载 CA 证书、不注入 HTTP(S)_PROXY。
 *
 * <p><b>取消语义</b>：kill() 停容器，follow 日志流随之关闭。终态（onComplete）直接取累积内容，
 * 读流仅做一次合并的短兜底排空（STREAM_DRAIN 秒）；超时与取消用 killed 标志区分。
 *
 * <p>本类实例为单次使用（持有累积缓冲区与 killed 状态），每次执行新建一个实例。
 */
public final class DockerCommandRunner implements CommandRunner {

    private static final String DOCKER_IMAGE = "ghcr.io/runify-dev/hush-toolbox";
    /**
     * 拉镜像用独立超时，不占用命令执行的时间预算
     */
    private static final int IMAGE_PULL_TIMEOUT = 600;

    /**
     * 取消/退出后读流的合并兜底排空时长（秒），也用作 stopContainer 的优雅停止超时。
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

    public DockerCommandRunner(String command, Map<String, String> env, String workDir, int timeout) {
        this.command = command;
        this.env = env;
        this.workDir = workDir;
        this.timeout = timeout;
    }

    @Override
    public void run(Listener listener) {
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

                stdoutFuture = followLog(client, containerId, true, listener::onNext);
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
                    listener.onError(new RuntimeException("命令执行超时（" + timeout + "秒）"));
                    return;
                }

                int exitCode = waitCallback.awaitStatusCode();

                // 合并短兜底排空读流；完不完成都不阻塞终态，直接取累积内容
                drainStreams(stdoutFuture, stderrFuture);
                String stdout, stderr;
                synchronized (stdoutSb) {
                    stdout = stdoutSb.toString();
                }
                synchronized (stderrSb) {
                    stderr = stderrSb.toString();
                }
                listener.onComplete(new CommandResult(exitCode, stdout, stderr));
            } catch (Exception e) {
                if (dc != null && cid != null) {
                    stopContainer(dc, cid);
                }
                listener.onError(e);
            } finally {
                // 不阻塞等待读流；仅吞掉异常完成。容器随后手动删除，follow 流自然结束
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
            }
        };

        try {
            DRIVER_EXECUTOR.execute(driver);
        } catch (RejectedExecutionException e) {
            // 队列满：在调用线程（工作流线程）上立即拒绝 → 走失败
            listener.onError(new RuntimeException("docker 执行队列已满，请稍后重试", e));
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

    // ---- docker 专属底层工具 ----

    /**
     * 合并等待读流排空，封顶 STREAM_DRAIN 秒；超时即放弃（已累积内容已在缓冲区中），不抛出。
     */
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