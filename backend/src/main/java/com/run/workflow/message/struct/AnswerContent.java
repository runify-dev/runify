package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

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
    private String id;
    private Map<String, Object> extra;


    public AnswerContent(ContentTypeConstants type,
                         INode<?, ?> node,
                         String workflowRunId,
                         String id) {
        super(type, workflowRunId);
        this.extra = new HashMap<>();
        this.extra.put("nodeId", node.getNode().getId());
        this.extra.put("nodeStatus", node.getStatus());
        this.extra.put("nodeName", node.getNode().getProperties().getString("name"));
        this.id = id;
    }

    public AnswerContent(ContentTypeConstants type,
                         String workflowRunId,
                         String id,
                         String nodeId,
                         NodeStatus status,
                         String nodeName) {
        super(type, workflowRunId);
        this.id = id;
        this.extra = new HashMap<>();
        this.extra.put("nodeId", nodeId);
        this.extra.put("nodeStatus", status);
        this.extra.put("nodeName", nodeName);
    }

    public AnswerContent(ContentTypeConstants type,
                         String workflowRunId,
                         String id,
                         Map<String, Object> extra
    ) {
        super(type, workflowRunId);
        this.id = id;
        this.extra = extra;
    }

}
