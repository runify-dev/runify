package com.run.workflow;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkflowRunRegistry {

    private static final Map<String, WorkFlowManage> RUNNING = new ConcurrentHashMap<>();

    /**
     * 注册
     *
     * @param runId 运行id
     * @param wm    实例
     */
    public static void register(String runId, WorkFlowManage wm) {
        if (runId == null || wm == null) {
            throw new IllegalArgumentException("runId and workflowManage must not be null");
        }
        RUNNING.put(runId, wm);
        log.debug("Workflow registered: {}, total running: {}", runId, RUNNING.size());
    }

    /**
     * 注销一个工作流(无论成功/失败/取消都应调用)。
     */
    public static void unregister(String runId) {
        if (runId == null) return;
        WorkFlowManage removed = RUNNING.remove(runId);
        if (removed != null) {
            log.debug("Workflow unregistered: {}, total running: {}", runId, RUNNING.size());
        }
    }

    /**
     * 取消某个运行时工作流
     *
     * @param runId 工作流运行id
     * @return 结果
     */
    public static CancelResult cancel(String runId) {
        if (runId == null) {
            return CancelResult.NOT_FOUND;
        }
        WorkFlowManage wm = RUNNING.get(runId);
        if (wm == null) {
            log.info("Cancel requested but workflow not found (may already finished): {}", runId);
            return CancelResult.NOT_FOUND;
        }
        try {
            wm.cancel();
            log.info("Cancel signal sent to workflow: {}", runId);
            return CancelResult.CANCELLED;
        } catch (Exception e) {
            log.error("Failed to cancel workflow: {}", runId, e);
            return CancelResult.FAILED;
        }
    }

    public static WorkFlowManage get(String runId) {
        return runId == null ? null : RUNNING.get(runId);
    }

    public static boolean isRunning(String runId) {
        return runId != null && RUNNING.containsKey(runId);
    }

    public static int runningCount() {
        return RUNNING.size();
    }

    public static Set<String> runningIds() {
        return Collections.unmodifiableSet(Set.copyOf(RUNNING.keySet()));
    }

    /**
     * 取消操作结果
     */
    public enum CancelResult {
        /**
         * 取消信号已发送
         */
        CANCELLED,
        /**
         * 工作流不存在(可能已结束)
         */
        NOT_FOUND,
        /**
         * 取消过程中发生异常
         */
        FAILED
    }
}