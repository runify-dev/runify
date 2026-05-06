package com.run.workflow.message.aggregator.impl;

import com.run.workflow.message.aggregator.ContentAggregator;
import com.run.workflow.message.struct.QuestionContent;
import com.run.workflow.message.struct.ToolCallContent;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  14:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ToolCallAggregator implements ContentAggregator<ToolCallContent> {

    @Override
    public ToolCallContent apply(ToolCallContent prev, ToolCallContent chunk) {
        if (prev == null) {
            return chunk;
        }

        ToolCallContent result = new ToolCallContent();

        // 合并基础字段
        mergeBaseFields(prev, chunk, result);

        // 合并 content
        String prevContent = prev.getContent() != null ? prev.getContent() : "";
        String chunkContent = chunk.getContent() != null ? chunk.getContent() : "";
        result.setContent(prevContent + chunkContent);

        String prevArguments = prev.getFunctionArguments() != null ? prev.getFunctionArguments() : "";
        String chunkArguments = chunk.getFunctionArguments() != null ? chunk.getFunctionArguments() : "";
        result.setFunctionArguments(prevArguments + chunkArguments);

        String prevToolName = prev.getToolName() != null ? prev.getToolName() : "";
        String chunkToolName = chunk.getToolName() != null ? chunk.getToolName() : "";
        result.setToolName(prevToolName + chunkToolName);
        return result;
    }
}
