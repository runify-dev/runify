package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/30  17:10}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class AnswerContent extends Content {
    private NodeStatus status;
    private String realNodeId;
    private String nodeId;
    private String displayId;
    private String nodeName;


    public AnswerContent(ContentTypeConstants type, INode<?, ?> node, String workflowRunId) {
        super(type, workflowRunId);
        this.status = node.getStatus();
        this.realNodeId = node.getRealNodeId();
        this.nodeId = node.getNode().getId();
        this.displayId = node.getDisplayId();
        this.nodeName = node.getNode().getProperties().getString("name");
    }
}
