package com.run.common.run_command;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 命令执行的集中生命周期管理：在途 runner 注册表 + 优雅关停。
 *
 * <p>解决“应用关停时队列里尚未执行的 driver 被 {@code shutdownNow()} 丢弃 → 不回调终态”的残留：
 * 关停时<b>先主动 kill 所有在途/排队中的 runner</b>（它们会快速退出并各自回调 onComplete/onError），
 * 再对 driver 池 {@code shutdown()}（不是 shutdownNow），队列中的任务仍会被执行而非丢弃；
 * 待清空后才兜底 shutdownNow。
 *
 * <p>用法（二选一或并用）：
 * <ul>
 *   <li>在应用关停流程里显式调用 {@link #shutdownGracefully(long)}（推荐，时机可控）；</li>
 *   <li>或启动时调用一次 {@link #installShutdownHook()} 注册 JVM 关停钩子做兜底。</li>
 * </ul>
 * 注意：不要在 driver 线程内部调用本类的关停方法（会等待自身所在的池，导致死锁）。
 */
public final class CommandRunnerLifecycle {

    private CommandRunnerLifecycle() {
    }

    /**
     * 默认优雅关停宽限期（秒），可配 runify.terminal.shutdown.grace.seconds。
     */
    private static final int DEFAULT_GRACE_SECONDS =
            CommandRunner.resolveQueueCap("runify.terminal.shutdown.grace.seconds", 10);

    /**
     * 在途/排队中的 runner；run() 提交成功即登记，终态后注销。
     */
    private static final Set<CommandRunner> ACTIVE = ConcurrentHashMap.newKeySet();

    /**
     * 各 runner 实现的 driver 池，由实现类静态初始化时登记。
     */
    private static final List<ExecutorService> POOLS = new CopyOnWriteArrayList<>();

    private static final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private static final AtomicBoolean hookInstalled = new AtomicBoolean(false);

    // ---- 供实现类调用的内部 API（请勿在业务代码中使用）----

    /**
     * 实现类登记自己的 driver 池（静态初始化时调用一次）。
     */
    public static void registerPool(ExecutorService pool) {
        if (pool != null) {
            POOLS.add(pool);
        }
    }

    /**
     * 登记一个在途 runner。关停进行中则拒绝登记（返回 false，调用方应直接走失败）。
     */
    public static boolean track(CommandRunner runner) {
        if (shuttingDown.get()) {
            return false;
        }
        ACTIVE.add(runner);
        // 双重检查：登记后若恰好进入关停，撤销登记，避免漏 kill 之后又被遗留
        if (shuttingDown.get()) {
            ACTIVE.remove(runner);
            return false;
        }
        return true;
    }

    /**
     * 注销一个 runner（终态后调用，幂等）。
     */
    public static void untrack(CommandRunner runner) {
        ACTIVE.remove(runner);
    }

    /**
     * 是否正在关停（实现类可据此在 run() 入口快速短路）。
     */
    public static boolean isShuttingDown() {
        return shuttingDown.get();
    }

    // ---- 对外关停 API ----

    /**
     * 用默认宽限期优雅关停。
     */
    public static void shutdownGracefully() {
        shutdownGracefully(DEFAULT_GRACE_SECONDS);
    }

    /**
     * 优雅关停：保证所有已提交（含排队中）的命令都产生终态回调后再退出。幂等，仅首次生效。
     *
     * @param graceSeconds 等待 driver 池清空的总宽限期（秒）
     */
    public static void shutdownGracefully(long graceSeconds) {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }

        // 1) 停止接收新任务；shutdown() 不丢弃队列，已排队的 driver 仍会被执行
        for (ExecutorService pool : POOLS) {
            safe(pool::shutdown);
        }

        // 2) 主动取消所有在途/排队中的 runner —— 运行中的杀进程/停容器；
        //    排队中的会被置 killed，待其被调度执行时走 kill 抢跑分支，快速回调 onComplete
        for (CommandRunner r : ACTIVE) {
            safe(r::kill);
        }

        // 3) 等待 driver 池清空（被 kill 后各任务会迅速跑完并回调终态）
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(Math.max(0, graceSeconds));
        for (ExecutorService pool : POOLS) {
            long remain = deadlineNanos - System.nanoTime();
            if (remain <= 0) {
                break;
            }
            try {
                pool.awaitTermination(remain, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 4) 兜底：极个别仍未结束的强制关停（此时应已全部回调）
        for (ExecutorService pool : POOLS) {
            safe(pool::shutdownNow);
        }

        // 5) 最后关停读流虚拟线程池（中断可能残留的读线程）
        safe(CommandRunner.STREAM_EXECUTOR::shutdownNow);
    }

    /**
     * 注册 JVM 关停钩子作为兜底（用默认宽限期）。幂等，多次调用只装一次。
     */
    public static void installShutdownHook() {
        installShutdownHook(DEFAULT_GRACE_SECONDS);
    }

    /**
     * 注册 JVM 关停钩子作为兜底。注意：{@code kill -9} / taskkill /F 不会触发钩子。
     */
    public static void installShutdownHook(long graceSeconds) {
        if (hookInstalled.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> shutdownGracefully(graceSeconds), "runify-cmd-shutdown"));
        }
    }

    private static void safe(Runnable action) {
        try {
            action.run();
        } catch (Throwable ignored) {
        }
    }
}