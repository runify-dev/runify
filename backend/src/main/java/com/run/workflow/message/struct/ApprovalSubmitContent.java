package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.common.util.CommonUtils;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
@NoArgsConstructor
public class ApprovalSubmitContent extends Content {
    private String content;
    private String result;

    public ApprovalSubmitContent(String content, String result, Position position, String workflowRunId) {
        super(ContentTypeConstants.APPROVAL_SUBMIT, workflowRunId, CommonUtils.uuid7().toString(), new HashMap<>());
        this.content = content;
        this.result = result;
        this.setPosition(position);
    }

    public static ApprovalSubmitContent of(JsonObject content, String workflowRunId) {
        String text = content.getString("content", "");
        String result = content.getString("result", "");
        Position position = null;
        JsonObject posJson = content.getJsonObject("position");
        if (posJson != null) {
            position = posJson.mapTo(Position.class);
        }
        return new ApprovalSubmitContent(text, result, position, workflowRunId);
    }

    @Override
    public String toString() {
        return content;
    }
}
