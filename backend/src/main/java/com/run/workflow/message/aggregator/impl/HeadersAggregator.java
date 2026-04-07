package com.run.workflow.message.aggregator.impl;

import com.run.workflow.message.aggregator.ContentAggregator;
import com.run.workflow.message.struct.HeadersContent;
import com.run.workflow.message.struct.HeadersContent;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  14:44}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

public class HeadersAggregator implements ContentAggregator<HeadersContent> {

    @Override
    public HeadersContent apply(HeadersContent prev, HeadersContent chunk) {
        if (prev == null) {
            return chunk;
        }

        // 合并 Map：后来的 chunk 覆盖相同 key 的值
        Map<String, String> mergedContent = new HashMap<>();

        // 先放入 prev 的所有内容
        if (prev.getContent() != null) {
            mergedContent.putAll(prev.getContent());
        }

        // 再放入 chunk 的内容（会覆盖相同 key 的值）
        if (chunk.getContent() != null) {
            mergedContent.putAll(chunk.getContent());
        }

        // 创建新的 HeadersContent 对象
        return new HeadersContent(
                mergedContent,
                chunk.getWorkflowRunId() != null ? chunk.getWorkflowRunId() : prev.getWorkflowRunId(),
                chunk.getId() != null ? chunk.getId() : prev.getId(),
                chunk.getExtra() != null ? chunk.getExtra() : prev.getExtra()
        );
    }
}