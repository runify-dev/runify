package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/2/5  22:55}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class StatusContent extends AnswerContent {
    private Integer content;

    public StatusContent(Integer content, INode<?, ?> node, String workflowRunId) {
        super(ContentTypeConstants.STATUS, node, workflowRunId);
        this.content = content;
    }

    @Override
    public String toString() {
        return content.toString();
    }
}
