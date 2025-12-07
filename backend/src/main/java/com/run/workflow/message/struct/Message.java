package com.run.workflow.message.struct;


import com.run.common.constants.MessageConstants;

import java.util.List;

public record Message(MessageConstants type, List<Content> content) {

}
