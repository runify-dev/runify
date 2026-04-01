package com.run.workflow.message.struct.chunk;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.message.struct.AnswerContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;


@Getter
@Setter
@NoArgsConstructor
public class TextContentChunk extends AnswerContent {
    private String content;

    public TextContentChunk(String content, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.TEXT, node, workflowRunId, id);
        this.content = content;
    }

    public TextContentChunk(String content, String workflowRunId,
                            String id,
                            NodeStatus status,
                            String nodeId,
                            String nodeName) {
        super(ContentTypeConstants.TEXT, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
    }

    public TextContentChunk(String content, String workflowRunId, String id, Map<String, Object> extra) {
        super(ContentTypeConstants.TEXT, workflowRunId, id, extra);
        this.content = content;
    }

}
