package com.run.common.run_command.impl;

import com.run.common.run_command.CommandResult;
import com.run.common.run_command.CommandRunner;
import com.run.common.run_command.CommandRunnerLifecycle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 本地进程运行时。
 * driver 池：平台线程，大小 = 并发上限 = CONCURRENCY（默认 64，可配）；有界队列排队，满则拒绝。
 * 读流走共享虚拟线程池 STREAM_EXECUTOR。
 *
 * <p><b>exactly-once 终态保证（结构性）</b>：不再逐路径论证“一定会回调”，而是用实例级
 * {@code terminated} 守卫 + {@code catch (Throwable)} + finally 终极兜底，从结构上保证
 * 每次执行<b>恰好</b>调用一次 onComplete 或 onError——无论被 kill、抛 Error、还是任何竞态。
 *
 * <p><b>取消语义</b>：kill() 强杀进程树并主动关流；终态直接取“边读边累积”的缓冲区内容，不以读流
 * future 完成为硬门槛，读流仅做一次合并短兜底排空（STREAM_DRAIN 秒）。onNext 经 {@code terminated}
 * 守卫，保证不会在终态之后再触发（残留读虚拟线程的迟到分片被丢弃）。
 *
 * <p>本类实例为单次使用，每次执行新建一个实例。
 */
public final class LocalCommandRunner implements CommandRunner {

    /** 取消/退出后读流的合并兜底排空时长（秒）。 */
    private static final int STREAM_DRAIN = CommandRunner.resolveQueueCap("runify.terminal.drain.seconds", 2);

    /**
     * 本地命令并发上限（= 同时在跑的子进程数）。
     * driver 平台线程几乎只 park 在 waitFor() 上等子进程退出、不吃 CPU，真正的计算/网络在子进程里，
     * 读流又走虚拟线程池 STREAM_EXECUTOR；因此这个值与核数无关，远高于 CORES 也安全。
     * 真实天花板是 fd（ulimit -n）/ PID / 内存（每条命令两个累积 StringBuilder），而非 CPU。
     * 可配 runify.terminal.local.concurrency，默认 64。
     * 注意：ffmpeg 等 CPU 重活也共用本池，其并发建议在 Skill/编排层单独约束，勿靠此通用上限。
     */
    private static final int CONCURRENCY =
            CommandRunner.resolveQueueCap("runify.terminal.local.concurrency", 64);

    /**
     * 本地 driver 池：core == max == CONCURRENCY，超出的任务进有界队列排队，队列满 → AbortPolicy 抛异常
     */
    private static final ThreadPoolExecutor DRIVER_EXECUTOR = new ThreadPoolExecutor(
            CONCURRENCY, CONCURRENCY,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(CommandRunner.resolveQueueCap("runify.terminal.local.queue", 500)),
            CommandRunner.namedFactory("runify-local"),
            new ThreadPoolExecutor.AbortPolicy());

    static {
        // 登记本池，供优雅关停统一管理
        CommandRunnerLifecycle.registerPool(DRIVER_EXECUTOR);
    }

    private final String command;
    private final Map<String, String> env;
    private final Path workingDir;
    private final int timeout;

    private volatile Process process;
    /** 标记是否被外部 kill；用于区分“超时”与“取消”，并支持 kill 抢跑（进程尚未创建即被取消）。 */
    private volatile boolean killed = false;
    /** exactly-once 终态守卫：onComplete / onError / onNext 全部经它把关。 */
    private final AtomicBoolean terminated = new AtomicBoolean(false);
    /** 单次使用守卫：run() 不可重复调用（实例与累积缓冲区、terminated 一一对应）。 */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /** 边读边累积，onComplete 时直接取用，不依赖读流 future 是否完成。 */
    private final StringBuilder stdoutSb = new StringBuilder();
    private final StringBuilder stderrSb = new StringBuilder();

    public LocalCommandRunner(String command, Map<String, String> env, Path workingDir, int timeout) {
        this.command = command;
        this.env = env;
        this.workingDir = workingDir;
        this.timeout = timeout;
    }

    @Override
    public void run(Listener listener) {
        // 单次使用：重复 run() 直接拒绝，避免共享 terminated 导致第二次执行不回调
        if (!started.compareAndSet(false, true)) {
            listener.onError(new IllegalStateException("LocalCommandRunner 为单次使用，run() 不可重复调用"));
            return;
        }

        // onNext 守卫：终态之后丢弃迟到分片，避免 onNext 晚于 onComplete/onError
        Consumer<String> emit = chunk -> {
            if (!terminated.get()) {
                listener.onNext(chunk);
            }
        };

        Runnable driver = () -> {
            CompletableFuture<Void> stdoutFuture = null;
            CompletableFuture<Void> stderrFuture = null;
            try {
                ProcessBuilder pb = new ProcessBuilder();
                pb.directory(workingDir.toFile());
                if (env != null && !env.isEmpty()) {
                    pb.environment().putAll(env);
                }
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    pb.command("powershell", "-NoProfile", "-NonInteractive", "-Command", command);
                } else {
                    pb.command("sh", "-c", command);
                }
                pb.redirectErrorStream(false);

                Process p = pb.start();
                this.process = p;

                // kill 抢跑保护：进程在队列里排队期间被取消，create 之后立即处理
                if (killed) {
                    killProcessTree(p);
                    closeStreams(p);
                }

                stdoutFuture = readStreamAsync(p.getInputStream(), stdoutSb, emit);
                stderrFuture = readStreamAsync(p.getErrorStream(), stderrSb, emit);

                // kill() 会强杀进程并关流，waitFor 随即返回；因此取消由 kill 触发、走 onComplete
                boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);
                if (!finished && !killed) {
                    // 真·超时（非取消）：杀树 + 关流 + 报错
                    killProcessTree(p);
                    closeStreams(p);
                    fail(listener, new RuntimeException("命令执行超时（" + timeout + "秒）"));
                    return;
                }

                // 合并短兜底排空读流；完不完成都不阻塞终态，直接取累积内容
                drainStreams(stdoutFuture, stderrFuture);
                String stdout, stderr;
                synchronized (stdoutSb) {
                    stdout = stdoutSb.toString();
                }
                synchronized (stderrSb) {
                    stderr = stderrSb.toString();
                }
                complete(listener, new CommandResult(safeExitCode(p), stdout, stderr));
            } catch (Throwable t) {
                // 注意：Throwable，连 Error 一起兜住，避免 driver 线程静默死亡而不回调
                Process p = this.process;
                if (p != null && p.isAlive()) {
                    killProcessTree(p);
                    closeStreams(p);
                }
                fail(listener, t);
            } finally {
                // 不阻塞等待读流：虚拟线程 + 流已关闭，会随子进程退出自然结束；仅吞异常完成
                if (stdoutFuture != null) stdoutFuture.exceptionally(ex -> null);
                if (stderrFuture != null) stderrFuture.exceptionally(ex -> null);
                // 终极兜底：任何路径都没产生终态（含 onComplete 自身抛异常的极端情况）→ 兜一个 onError
                if (terminated.compareAndSet(false, true)) {
                    listener.onError(new IllegalStateException("命令 driver 结束但未产生终态回调（结构性兜底触发）"));
                }
                CommandRunnerLifecycle.untrack(this);
            }
        };

        // 提交前登记；关停进行中则直接拒绝，保证不会有“提交了却无人 kill”的窗口
        if (!CommandRunnerLifecycle.track(this)) {
            fail(listener, new RuntimeException("服务正在关停，已拒绝新的本地命令"));
            return;
        }
        try {
            DRIVER_EXECUTOR.execute(driver);
        } catch (RejectedExecutionException e) {
            // 队列满 / 池已关闭：在调用线程上立即拒绝 → 走失败
            CommandRunnerLifecycle.untrack(this);
            fail(listener, new RuntimeException("本地执行队列已满，请稍后重试", e));
        } catch (Throwable t) {
            // 提交阶段的任何其它异常：driver 未启动、finally 兜底不存在，这里必须补一个终态
            CommandRunnerLifecycle.untrack(this);
            fail(listener, t);
        }
    }

    @Override
    public void kill() {
        killed = true;
        Process p = this.process;
        if (p != null) {
            if (p.isAlive()) {
                killProcessTree(p);
            }
            // 关键：主动关流唤醒阻塞在 read() 上的读线程；前台命令会立即 EOF，脱离子进程由短兜底兜住
            closeStreams(p);
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

    // ---- 本地专属底层工具 ----

    /** 进程已退出取真实退出码；万一仍未退出（取消竞态）则返回 -1，避免 exitValue() 抛异常。 */
    private static int safeExitCode(Process p) {
        try {
            return p.exitValue();
        } catch (IllegalThreadStateException e) {
            return -1;
        }
    }

    /**
     * 杀掉进程及其所有子进程（进程树）。
     * destroyForcibly() 只杀直接子进程（sh），sh 下面的实际命令会成为孤儿进程继续运行。
     */
    private static void killProcessTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    /**
     * 关闭进程的 stdout/stderr 流，尝试唤醒阻塞读线程。幂等、吞异常。
     */
    private static void closeStreams(Process process) {
        try {
            process.getInputStream().close();
        } catch (Exception ignored) {
        }
        try {
            process.getErrorStream().close();
        } catch (Exception ignored) {
        }
    }

    /**
     * 合并等待读流排空，封顶 STREAM_DRAIN 秒；超时即放弃（已累积内容已在缓冲区中），不抛出。
     */
    private static void drainStreams(CompletableFuture<?>... futures) {
        try {
            CompletableFuture.allOf(futures).get(STREAM_DRAIN, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    /**
     * 异步读取输入流：边读边累积到 sink，并实时回调每个分片（在共享虚拟线程池上）。
     * 流被关闭/进程退出导致的 IOException 属预期，直接吞掉——已累积内容保留在 sink 中。
     */
    private static CompletableFuture<Void> readStreamAsync(InputStream is, StringBuilder sink, Consumer<String> onChunk) {
        return CompletableFuture.runAsync(() -> {
            try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                char[] buf = new char[4096];
                int n;
                while ((n = reader.read(buf)) != -1) {
                    String chunk = new String(buf, 0, n);
                    synchronized (sink) {
                        sink.append(chunk);
                    }
                    if (onChunk != null) {
                        onChunk.accept(chunk);
                    }
                }
            } catch (IOException ignored) {
                // 进程退出 / 被 kill 关流，属预期；已读内容已在 sink 中
            }
        }, STREAM_EXECUTOR);
    }
}