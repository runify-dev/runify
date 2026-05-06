package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ToolCallContent extends Content {
    private String content;
    private NodeStatus status;
    private String functionArguments;
    private String toolName;

    public ToolCallContent(String toolName, String content, String functionArguments, NodeStatus status, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.TOOL, node, workflowRunId, id);
        this.status = status;
        this.content = content;
        this.functionArguments = functionArguments;
        this.toolName = toolName;
    }


    @Override
    public String toString() {
        return content;
    }
}
