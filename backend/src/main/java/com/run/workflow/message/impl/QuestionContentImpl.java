package com.run.workflow.message.impl;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.QuestionContent;
import io.vertx.core.json.JsonObject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/30  18:49}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class QuestionContentImpl {
    public static Boolean support(ContentTypeConstants type) {
        return type == ContentTypeConstants.QUESTION;
    }


    public static Content newInstance(JsonObject jsonObject) {
        return jsonObject.mapTo(QuestionContent.class);
    }

}
