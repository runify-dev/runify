package com.run.workflow.message.aggregator.impl;

import com.run.workflow.message.aggregator.ContentAggregator;
import com.run.workflow.message.struct.ReasoningContent;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  14:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ReasoningAggregator implements ContentAggregator<ReasoningContent> {

    @Override
    public ReasoningContent apply(ReasoningContent prev, ReasoningContent chunk) {
        if (prev == null) {
            return chunk;
        }

        ReasoningContent result = new ReasoningContent();

        // 合并基础字段
        mergeBaseFields(prev, chunk, result);

        // 合并 content
        String prevContent = prev.getContent() != null ? prev.getContent() : "";
        String chunkContent = chunk.getContent() != null ? chunk.getContent() : "";
        result.setContent(prevContent + chunkContent);

        // 合并 status: 优先使用 chunk 的，否则使用 prev 的
        result.setStatus(chunk.getStatus() != null ? chunk.getStatus() : prev.getStatus());

        return result;
    }
}