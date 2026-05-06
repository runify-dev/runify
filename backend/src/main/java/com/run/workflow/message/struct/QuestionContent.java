package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.common.pojo.File;
import com.run.common.util.CommonUtils;
import com.run.handler.application.vo.ConversationVO;
import com.run.workflow.NodeStatus;
import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/30  17:13}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class QuestionContent extends Content {
    private String content;
    private List<File> images;
    private List<File> videos;
    private List<File> files;
    private List<String> texts;

    public QuestionContent(String content, List<File> images, List<File> videos, List<File> files, List<String> texts, String workflowRunId) {
        super(ContentTypeConstants.QUESTION, workflowRunId, CommonUtils.uuid7().toString(), new HashMap<>());
        this.content = content;
        this.images = images;
        this.videos = videos;
        this.files = files;
        this.texts = texts;
    }

    public static QuestionContent of(JsonObject content, String workflowRunId) {
        String text = content.getString("content", "");
        List<File> images = content.getJsonArray("images") != null
                ? content.getJsonArray("images").getList() : List.of();
        List<File> videos = content.getJsonArray("videos") != null
                ? content.getJsonArray("videos").getList() : List.of();
        List<File> files = content.getJsonArray("files") != null
                ? content.getJsonArray("files").getList() : List.of();
        List<String> texts = content.getJsonArray("texts") != null
                ? content.getJsonArray("texts").getList() : List.of();
        return new QuestionContent(text, images, videos, files, texts, workflowRunId);
    }

    @Override
    public String toString() {
        return content;
    }
}
