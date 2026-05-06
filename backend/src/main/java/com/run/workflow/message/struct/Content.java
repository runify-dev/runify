package com.run.workflow.message.struct;


import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class Content {
    private String id;

    private Map<String, Object> extra;

    private ContentTypeConstants type;

    private String workflowRunId;

    private Position position;

    public Content(ContentTypeConstants type,
                   INode<?, ?> node,
                   String workflowRunId,
                   String id) {
        this.type = type;
        this.workflowRunId = workflowRunId;
        this.extra = new HashMap<>();
        Map<String, Object> nodeInfo = new HashMap<>();
        nodeInfo.put("id", node.getNode().getId());
        nodeInfo.put("status", node.getStatus());
        nodeInfo.put("name", node.getNode().getProperties().getString("name"));
        this.position = new Position(node.getNode().getId());
        this.extra.put("nodeInfo", nodeInfo);
        this.id = id;
    }

    public Content(ContentTypeConstants type,
                   String workflowRunId,
                   String id,
                   String nodeId,
                   NodeStatus status,
                   String nodeName) {
        this.type = type;
        this.workflowRunId = workflowRunId;
        this.id = id;
        this.extra = new HashMap<>();
        Map<String, Object> nodeInfo = new HashMap<>();
        nodeInfo.put("id", nodeId);
        nodeInfo.put("status", status);
        nodeInfo.put("name", nodeName);
        this.extra.put("nodeInfo", nodeInfo);
    }

    public Content(ContentTypeConstants type,
                   String workflowRunId,
                   String id,
                   Map<String, Object> extra
    ) {
        this.type = type;
        this.workflowRunId = workflowRunId;
        this.id = id;
        this.extra = extra;
    }
}
