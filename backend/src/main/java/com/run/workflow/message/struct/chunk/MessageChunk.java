package com.run.workflow.message.struct.chunk;

import com.run.common.constants.MessageConstants;
import com.run.workflow.message.struct.AnswerContent;

import java.util.List;


public record MessageChunk(MessageConstants type, List<AnswerContent> content) {
}
