package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.common.util.CommonUtils;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.ToolCallMeta;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ToolCallContent extends Content {
    private String content;
    private String reasoning;
    private String aiId;
    private String reasoningKey;
    private NodeStatus status;
    private String functionArguments;
    private String toolName;

    public ToolCallContent(String toolName, String content, String functionArguments, NodeStatus status, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.TOOL, node, workflowRunId, id);
        this.status = status;
        this.content = content;
        this.functionArguments = functionArguments;
        this.toolName = toolName;
        this.aiId = CommonUtils.uuid7().toString();
        this.reasoning = "";
        this.reasoningKey = "reasoning_content";
    }

    public ToolCallContent(String toolName, String content, String functionArguments, String reasoning, String reasoningKey, String aiId, NodeStatus status, INode<?, ?> node, String workflowRunId, String id) {
        super(ContentTypeConstants.TOOL, node, workflowRunId, id);
        this.status = status;
        this.content = content;
        this.functionArguments = functionArguments;
        this.toolName = toolName;
        this.reasoning = reasoning;
        this.reasoningKey = reasoningKey;
        this.aiId = aiId;
    }


    /**
     * 填充 reasoning/aiId/reasoningKey（链式调用）
     */
    public ToolCallContent withMeta(ToolCallMeta meta) {
        this.reasoning = meta.reasoning();
        this.reasoningKey = meta.reasoningKey();
        this.aiId = meta.aiId();
        return this;
    }

    @Override
    public String toString() {
        return content;
    }
}
