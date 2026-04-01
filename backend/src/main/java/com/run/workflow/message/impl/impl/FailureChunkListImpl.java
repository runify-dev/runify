package com.run.workflow.message.impl.impl;


import com.run.common.constants.ContentTypeConstants;
import com.run.common.util.CommonUtils;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.message.struct.chunk.FailureContentChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FailureChunkListImpl {
    public static Boolean support(ContentTypeConstants type) {
        return type == ContentTypeConstants.FAILURE;
    }

    public static List<Content> toBlock(List<FailureContentChunk> self) {
        List<Content> result = new ArrayList<>();
        LinkedHashMap<String, List<FailureContentChunk>> r = self.stream().collect(Collectors.groupingBy(FailureContentChunk::getId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<FailureContentChunk>> stringListEntry : r.entrySet()) {
            List<FailureContentChunk> value = stringListEntry.getValue();
            FailureContentChunk first = value.getFirst();
            String collect = value.stream().map(FailureContentChunk::getContent).collect(Collectors.joining());
            FailureContent failureContent = new FailureContent();
            CommonUtils.copyProperties(first, failureContent);
            failureContent.setContent(collect);
            result.add(failureContent);
        }
        return result;
    }
}
