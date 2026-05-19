package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.common.util.CommonUtils;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class SystemContent extends Content {
    private String content;

    public SystemContent(String content, String workflowRunId) {
        super(ContentTypeConstants.SYSTEM, workflowRunId, CommonUtils.uuid7().toString(), new HashMap<>());
        this.content = content;
    }

    public SystemContent(String content, String workflowRunId,
                         String id,
                         NodeStatus status,
                         String nodeId,
                         String nodeName) {
        super(ContentTypeConstants.SYSTEM, workflowRunId, id, nodeId, status, nodeName);
        this.content = content;
    }

    public SystemContent(String content, String workflowRunId, String id, Map<String, Object> extra) {
        super(ContentTypeConstants.SYSTEM, workflowRunId, id, extra);
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
