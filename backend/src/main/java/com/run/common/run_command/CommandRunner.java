package com.run.common.run_command;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 运行时抽象（本地进程 / Docker 容器）。自驱执行：
 * run() 启动后异步跑到结束，通过 Listener 回调结果，不认识 WorkFlowManage / ToolCallContent。
 *
 * <p>并发治理：driver（命令生命周期）由各实现自己的平台线程池限流（池大小=并发上限，
 * 有界队列排队，满则拒绝）；读流（stdout/stderr）走共享的虚拟线程池 STREAM_EXECUTOR，不限流。
 */
public interface CommandRunner {

    /**
     * 异步启动并自驱到结束；结束时恰好回调一次 onComplete（正常退出）或 onError（异常/超时/拒绝）。
     */
    void run(Listener listener);

    /**
     * 强制中止（取消）。必须幂等。kill 后进程/容器退出，driver 会照常回调 onComplete。
     */
    void kill();

    /**
     * 执行过程回调。onNext 在虚拟线程上被多次调用；onComplete / onError 恰好其一被调用一次。
     */
    interface Listener {
        /**
         * stdout 实时分片
         */
        void onNext(String stdoutChunk);

        /**
         * 进程/容器正常退出（携带退出码与完整 stdout/stderr，退出码非 0 也走这里）
         */
        void onComplete(CommandResult result);

        /**
         * 异常 / 超时 / 队列已满被拒
         */
        void onError(Throwable e);
    }

    // ---- 两个 runner 共享的基础设施 ----

    /**
     * 读流用：海量短命 IO 阻塞，虚拟线程主场，不限流（并发上限由各 runner 的 driver 池控制）
     */
    ExecutorService STREAM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 收割读流 future，防止线程/回调泄漏。
     * 进程退出/容器删除后管道关闭，read() 很快返回，给 5 秒兜底。
     */
    static void cleanupFuture(CompletableFuture<?> future) {
        if (future == null) return;
        future.exceptionally(ex -> null);  // 吞掉预期的 IOException
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    /**
     * 守护线程工厂，带前缀编号；driver 用平台线程，设为 daemon 不拦 JVM 退出。
     */
    static ThreadFactory namedFactory(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, prefix + "-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }

    /**
     * driver 池队列容量：系统属性 &gt; 环境变量（点转下划线大写）&gt; 默认值。
     */
    static int resolveQueueCap(String key, int def) {
        String v = System.getProperty(key);
        if (v == null) {
            v = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        if (v == null) return def;
        try {
            return Math.max(1, Integer.parseInt(v.trim()));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}