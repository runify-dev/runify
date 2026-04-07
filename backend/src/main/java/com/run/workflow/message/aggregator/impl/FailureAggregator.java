package com.run.workflow.message.aggregator.impl;

import com.run.workflow.message.aggregator.ContentAggregator;
import com.run.workflow.message.struct.FailureContent;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  14:44}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

public class FailureAggregator implements ContentAggregator<FailureContent> {

    @Override
    public FailureContent apply(FailureContent prev, FailureContent chunk) {
        if (prev == null) {
            return chunk;
        }

        FailureContent result = new FailureContent();

        // 合并基础字段
        mergeBaseFields(prev, chunk, result);

        // 合并 content
        String prevContent = prev.getContent() != null ? prev.getContent() : "";
        String chunkContent = chunk.getContent() != null ? chunk.getContent() : "";
        result.setContent(prevContent + chunkContent);

        return result;
    }
}