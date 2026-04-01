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
public class TextContent extends AnswerContent {
    private String content;

    public TextContent(String content, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.TEXT, node, workflowRunId, id);
        this.content = content;
    }

    public TextContent(String content, String workflowRunId,
                       String id,
                       NodeStatus status,
                       String nodeId,
                       String nodeName) {
        super(ContentTypeConstants.TEXT, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
    }

    public TextContent(String content, String workflowRunId, String id, Map<String, Object> extra) {
        super(ContentTypeConstants.TEXT, workflowRunId, id, extra);
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
