package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApprovalContent extends Content {
    private String content;
    private NodeStatus status;

    public ApprovalContent(String content, NodeStatus status, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.APPROVAL, node, workflowRunId, id);
        this.content = content;
        this.status = status;
    }

    @Override
    public String toString() {
        return content;
    }
}
