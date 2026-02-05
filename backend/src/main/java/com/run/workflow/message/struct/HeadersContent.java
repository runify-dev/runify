package com.run.workflow.message.struct;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/2/5  22:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class HeadersContent extends AnswerContent {
    private Map<String, String> content;

    public HeadersContent(Map<String, String> content, INode<?, ?> node, String workflowRunId) {
        super(ContentTypeConstants.HEADERS, node, workflowRunId);
        this.content = content;
    }

    @Override
    public String toString() {
        return content.toString();
    }
}
