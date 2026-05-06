package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class BreakContent extends Content {
    private Boolean content;

    public BreakContent(Boolean content, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.BREAK, node, workflowRunId, id);
        this.content = content;
    }

    public BreakContent(Boolean content, String workflowRunId,
                        String id,
                        NodeStatus status,
                        String nodeId,
                        String nodeName) {
        super(ContentTypeConstants.BREAK, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
    }

    public BreakContent(Boolean content, String workflowRunId, String id, Map<String, Object> extra) {
        super(ContentTypeConstants.BREAK, workflowRunId, id, extra);
        this.content = content;
    }

    @Override
    public String toString() {
        return content.toString();
    }
}