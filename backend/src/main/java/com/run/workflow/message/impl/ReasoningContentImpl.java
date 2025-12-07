package com.run.workflow.message.impl;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.ReasoningContent;
import io.vertx.core.json.JsonObject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/30  18:58}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ReasoningContentImpl {
    public static Boolean support(ContentTypeConstants type) {
        return type == ContentTypeConstants.REASONING;
    }


    public static Content newInstance(JsonObject jsonObject) {
        return jsonObject.mapTo(ReasoningContent.class);
    }


}
