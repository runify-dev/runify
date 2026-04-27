package com.run.workflow.message.struct;


import java.util.List;

public record Message(List<Content> content, long index) {

}
