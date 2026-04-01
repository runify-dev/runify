package com.run.workflow.message.struct.chunk;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.message.struct.AnswerContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

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

    public FailureContentChunk(String content, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.FAILURE, node, workflowRunId, id);
        this.content = content;
    }

    public FailureContentChunk(String content, String workflowRunId,
                               String id,
                               NodeStatus status,
                               String nodeId,
                               String nodeName) {
        super(ContentTypeConstants.FAILURE, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
    }

    public FailureContentChunk(String content, String workflowRunId, String id, Map<String, Object> extra) {
        super(ContentTypeConstants.FAILURE, workflowRunId, id, extra);
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}