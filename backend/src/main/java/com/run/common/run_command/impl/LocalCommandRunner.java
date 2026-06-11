package com.run.common.run_command.impl;

import com.run.common.run_command.CommandResult;
import com.run.common.run_command.CommandRunner;

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
import java.util.function.Consumer;

/**
 * 本地进程运行时。
 * driver 池：平台线程，大小 = 并发上限 = max(2, cores)；有界队列排队，满则拒绝。
 * 读流走共享虚拟线程池 STREAM_EXECUTOR。
 *
 * <p><b>取消语义</b>：kill() 强杀进程树并主动关闭进程的 stdout/stderr 流。终态（onComplete）
 * 直接取“边读边累积”的缓冲区内容，<b>不以读流 future 完成为硬门槛</b>；读流仅做一次合并的短兜底
 * 排空（STREAM_DRAIN 秒，可配 runify.terminal.drain.seconds）。这样即使有脱离/后台子进程仍持有
 * 管道写端、读线程无法立刻 EOF，kill 后也最多等 STREAM_DRAIN 秒即回调 onComplete，绝不会被命令
 * timeout 拖住。残留的读虚拟线程在子进程最终退出时自然结束，开销可忽略。
 *
 * <p>本类实例为单次使用（持有累积缓冲区与 killed 状态），每次执行新建一个实例。
 */
public final class LocalCommandRunner implements CommandRunner {

    private static final int CORES = Runtime.getRuntime().availableProcessors();

    /**
     * 取消/退出后读流的合并兜底排空时长（秒）。
     */
    private static final int STREAM_DRAIN = CommandRunner.resolveQueueCap("runify.terminal.drain.seconds", 2);

    /**
     * 本地 driver 池：core == max，超出核心数的任务进有界队列排队，队列满 → AbortPolicy 抛异常
     */
    private static final ThreadPoolExecutor DRIVER_EXECUTOR = new ThreadPoolExecutor(
            Math.max(2, CORES), Math.max(2, CORES),
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(CommandRunner.resolveQueueCap("runify.terminal.local.queue", 500)),
            CommandRunner.namedFactory("runify-local"),
            new ThreadPoolExecutor.AbortPolicy());

    private final String command;
    private final Map<String, String> env;
    private final Path workingDir;
    private final int timeout;

    private volatile Process process;
    /**
     * 标记是否被外部 kill；用于区分“超时”与“取消”，并支持 kill 抢跑（进程尚未创建即被取消）。
     */
    private volatile boolean killed = false;

    /**
     * 边读边累积，onComplete 时直接取用，不依赖读流 future 是否完成。
     */
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

                stdoutFuture = readStreamAsync(p.getInputStream(), stdoutSb, listener::onNext);
                stderrFuture = readStreamAsync(p.getErrorStream(), stderrSb, listener::onNext);

                // kill() 会强杀进程并关流，waitFor 随即返回；因此取消由 kill 触发、走 onComplete
                boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);
                if (!finished && !killed) {
                    // 真·超时（非取消）：杀树 + 关流 + 报错
                    killProcessTree(p);
                    closeStreams(p);
                    listener.onError(new RuntimeException("命令执行超时（" + timeout + "秒）"));
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
                listener.onComplete(new CommandResult(p.exitValue(), stdout, stderr));
            } catch (Exception e) {
                Process p = this.process;
                if (p != null && p.isAlive()) {
                    killProcessTree(p);
                    closeStreams(p);
                }
                listener.onError(e);
            } finally {
                // 不再阻塞等待：读线程是虚拟线程，流已关闭，会在子进程退出时自然结束；仅吞掉异常完成
                if (stdoutFuture != null) stdoutFuture.exceptionally(ex -> null);
                if (stderrFuture != null) stderrFuture.exceptionally(ex -> null);
            }
        };

        try {
            DRIVER_EXECUTOR.execute(driver);
        } catch (RejectedExecutionException e) {
            // 队列满：在调用线程（工作流线程）上立即拒绝 → 走失败
            listener.onError(new RuntimeException("本地执行队列已满，请稍后重试", e));
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
            // 关键：主动关流唤醒阻塞在 read() 上的读线程；前台命令会立即 EOF，脱离子进程则由短兜底兜住
            closeStreams(p);
        }
    }

    // ---- 本地专属底层工具 ----

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