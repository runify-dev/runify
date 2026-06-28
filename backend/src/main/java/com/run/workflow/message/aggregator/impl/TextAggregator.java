package com.run.workflow.message.aggregator.impl;

import com.run.workflow.message.aggregator.ContentAggregator;
import com.run.workflow.message.struct.TextContent;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  14:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TextAggregator implements ContentAggregator<TextContent> {

    @Override
    public TextContent apply(TextContent prev, TextContent chunk) {
        if (prev == null) {
            return chunk;
        }

        TextContent result = new TextContent();

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
