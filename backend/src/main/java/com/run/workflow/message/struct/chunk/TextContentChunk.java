package com.run.workflow.message.struct.chunk;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.message.struct.AnswerContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class TextContentChunk extends AnswerContent {
    private String content;

    public TextContentChunk(String content, INode<?, ?> node, String workflowRunId) {
        super(ContentTypeConstants.TEXT, node, workflowRunId);
        this.content = content;
    }


}
