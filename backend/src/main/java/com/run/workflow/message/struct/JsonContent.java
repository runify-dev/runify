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
public class JsonContent extends AnswerContent {
    private String content;

    public JsonContent(String content, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.JSON, node, workflowRunId, id);
        this.content = content;
    }

    public JsonContent(String content, String workflowRunId,
                       String id,
                       NodeStatus status,
                       String nodeId,
                       String nodeName) {
        super(ContentTypeConstants.JSON, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
    }

    public JsonContent(String content, String workflowRunId, String id, Map<String, Object> extra) {
        super(ContentTypeConstants.JSON, workflowRunId, id, extra);
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
