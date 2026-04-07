package com.run.workflow.message.aggregator.impl;

import com.run.workflow.message.aggregator.ContentAggregator;
import com.run.workflow.message.struct.QuestionContent;
import com.run.workflow.message.struct.QuestionContent;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  14:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class QuestionAggregator implements ContentAggregator<QuestionContent> {

    @Override
    public QuestionContent apply(QuestionContent prev, QuestionContent chunk) {
        if (prev == null) {
            return chunk;
        }

        QuestionContent result = new QuestionContent();

        // 合并基础字段
        mergeBaseFields(prev, chunk, result);

        // 合并 content
        String prevContent = prev.getContent() != null ? prev.getContent() : "";
        String chunkContent = chunk.getContent() != null ? chunk.getContent() : "";
        result.setContent(prevContent + chunkContent);

        return result;
    }
}
