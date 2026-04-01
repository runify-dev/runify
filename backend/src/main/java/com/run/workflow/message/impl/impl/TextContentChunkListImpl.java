package com.run.workflow.message.impl.impl;


import com.run.common.constants.ContentTypeConstants;
import com.run.common.util.CommonUtils;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.TextContent;
import com.run.workflow.message.struct.chunk.TextContentChunk;

import java.util.*;
import java.util.stream.Collectors;

public class TextContentChunkListImpl {
    public static Boolean support(ContentTypeConstants type) {
        return type == ContentTypeConstants.TEXT;
    }

    public static List<Content> toBlock(List<TextContentChunk> self) {
        List<Content> result = new ArrayList<>();
        LinkedHashMap<String, List<TextContentChunk>> r = self.stream()
                .collect(Collectors.groupingBy(TextContentChunk::getId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<TextContentChunk>> stringListEntry : r.entrySet()) {
            List<TextContentChunk> value = stringListEntry.getValue();
            TextContentChunk first = value.getFirst();
            String collect = value.stream().map(TextContentChunk::getContent).collect(Collectors.joining());
            TextContent content = new TextContent();
            CommonUtils.copyProperties(first, content);
            content.setContent(collect);
            result.add(content);
        }
        return result;
    }
}
