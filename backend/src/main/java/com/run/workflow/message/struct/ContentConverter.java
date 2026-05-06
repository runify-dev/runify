package com.run.workflow.message.struct;

import io.vertx.core.json.JsonObject;

/**
 * 根据 content.type 将 JsonObject 转换为对应的 Content 子类
 */
public class ContentConverter {

    public static Content of(JsonObject content, String workflowRunId) {
        String type = content.getString("type", "QUESTION");

        return switch (type) {
            case "APPROVAL_SUBMIT" -> ApprovalSubmitContent.of(content, workflowRunId);
            default -> QuestionContent.of(content, workflowRunId);
        };
    }
}
