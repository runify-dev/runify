package com.run.workflow.message.struct.chunk;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.message.struct.AnswerContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/2/2  23:34}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class FailureContentChunk extends AnswerContent {
    private String content;

    public FailureContentChunk(String content, INode<?, ?> node, String workflowRunId) {
        super(ContentTypeConstants.FAILURE, node, workflowRunId);
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}