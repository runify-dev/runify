package com.run.workflow.message.struct.chunk;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
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
    private String status;

    public ReasoningChunk(String content, NodeStatus status, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.REASONING, node, workflowRunId, id);
        this.content = content;
        this.status = status.name();
    }

    public ReasoningChunk(String content, NodeStatus status, String workflowRunId,
                          String id,
                          NodeStatus nodeStatus,
                          String nodeId,
                          String nodeName) {
        super(ContentTypeConstants.REASONING, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
        this.status = status.name();
    }

    @Override
    public String toString() {
        return content;
    }
}
