package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.common.util.CommonUtils;
import com.run.workflow.NodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
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

    public QuestionContent(String content, String workflowRunId) {
        super(ContentTypeConstants.QUESTION, workflowRunId, CommonUtils.uuid7().toString(), new HashMap<>());
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }
}
