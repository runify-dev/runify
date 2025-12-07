package com.run.workflow.message.impl;


import com.run.common.constants.ContentTypeConstants;
import com.run.common.util.CommonUtils;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.TextContent;
import com.run.workflow.message.struct.chunk.TextContentChunk;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class TextContentImpl {
    public static Boolean support(ContentTypeConstants type) {
        return type == ContentTypeConstants.TEXT;
    }

    public static List<TextContentChunk> toChunk(Content self) {
        TextContent s = (TextContent) self;
        TextContentChunk textContentChunk = new TextContentChunk();
        CommonUtils.copyProperties(s, textContentChunk);
        return List.of(textContentChunk);
    }

    public static Content newInstance(JsonObject jsonObject) {
        return jsonObject.mapTo(TextContent.class);
    }

    public static String toString(Content self) {
        TextContent s = (TextContent) self;
        return s.getContent();
    }
}
