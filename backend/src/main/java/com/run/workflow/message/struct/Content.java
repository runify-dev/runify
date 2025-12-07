package com.run.workflow.message.struct;


import com.run.common.constants.ContentTypeConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Content {
    private ContentTypeConstants type;

    private String workflowRunId;

    public Content(ContentTypeConstants type, String workflowRunId) {
        this.type = type;
        this.workflowRunId = workflowRunId;
    }
}
