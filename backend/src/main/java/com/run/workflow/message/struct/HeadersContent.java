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
 * {@code @Date: 2026/2/5  22:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class HeadersContent extends AnswerContent {
    private Map<String, String> content;

    public HeadersContent(Map<String, String> content, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.HEADERS, node, workflowRunId, id);
        this.content = content;
    }

    public HeadersContent(Map<String, String> content, String workflowRunId,
                          String id,
                          NodeStatus status,
                          String nodeId,
                          String nodeName) {
        super(ContentTypeConstants.HEADERS, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
    }

    public HeadersContent(Map<String, String> content, String workflowRunId, String id, Map<String, Object> extra) {
        super(ContentTypeConstants.HEADERS, workflowRunId, id, extra);
        this.content = content;
    }

    @Override
    public String toString() {
        return content.toString();
    }
}
