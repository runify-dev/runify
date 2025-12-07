package com.run.workflow.message.struct.chunk;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.message.struct.AnswerContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/30  18:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class ReasoningChunk extends AnswerContent {
    private String content;

    public ReasoningChunk(String content, INode<?, ?> node, String workflowRunId) {
        super(ContentTypeConstants.REASONING, node, workflowRunId);
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
