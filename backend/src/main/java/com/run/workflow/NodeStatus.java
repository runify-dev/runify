package com.run.workflow;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  21:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public enum NodeStatus {
    /**
     * 成功
     */
    SUCCESS,
    /**
     * 失败
     */
    FAIL,
    /**
     * 中断
     */
    INTERRUPT,
    /**
     * 运行中
     */
    RUNNING,
    /**
     * 运行前
     */
    BEFORE_RUNNING,
    /**
     * 已取消
     */
    CANCELLED
}
