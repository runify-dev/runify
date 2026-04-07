package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/30  18:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class ReasoningContent extends Content {
    private String content;
    private NodeStatus status;

    public ReasoningContent(String content, NodeStatus status, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.REASONING, node, workflowRunId, id);
        this.status = status;
        this.content = content;
    }

    public ReasoningContent(String content, String workflowRunId,
                            String id,
                            NodeStatus status,
                            String nodeId,
                            String nodeName) {
        super(ContentTypeConstants.REASONING, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
    }

    public ReasoningContent(String content, String workflowRunId, String id, Map<String, Object> extra) {
        super(ContentTypeConstants.REASONING, workflowRunId, id, extra);
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
