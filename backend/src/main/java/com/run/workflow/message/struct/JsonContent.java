package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JsonContent extends AnswerContent {
    private String content;

    public JsonContent(String content, INode<?, ?> node, String workflowRunId) {
        super(ContentTypeConstants.JSON, node, workflowRunId);
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
