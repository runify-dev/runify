package com.run.workflow.message.aggregator;

import com.run.workflow.message.struct.Content;

import java.util.function.BinaryOperator;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  14:44}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface ContentAggregator<T extends Content> extends BinaryOperator<T> {

    @Override
    T apply(T prev, T chunk);

    // 默认实现：合并基础字段
    default void mergeBaseFields(T prev, T chunk, T result) {
        // id: 优先使用 chunk 的 id，否则使用 prev 的 id
        result.setId(chunk.getId() != null ? chunk.getId() : prev.getId());

        // workflowRunId: 优先使用 chunk 的，否则使用 prev 的
        result.setWorkflowRunId(chunk.getWorkflowRunId() != null ?
                chunk.getWorkflowRunId() : prev.getWorkflowRunId());

        // extra: 优先使用 chunk 的，否则使用 prev 的
        result.setExtra(chunk.getExtra() != null ? chunk.getExtra() : prev.getExtra());

        result.setType(chunk.getType() != null ? chunk.getType() : prev.getType());
    }
}