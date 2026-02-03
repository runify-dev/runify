package com.run.workflow.message.impl.impl;


import com.run.common.constants.ContentTypeConstants;
import com.run.common.util.CommonUtils;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.ReasoningContent;
import com.run.workflow.message.struct.chunk.ReasoningChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReasoningChunkListImpl {
    public static Boolean support(ContentTypeConstants type) {
        return type == ContentTypeConstants.REASONING;
    }

    public static List<Content> toBlock(List<ReasoningChunk> self) {
        List<Content> result = new ArrayList<>();
        LinkedHashMap<String, List<ReasoningChunk>> r = self.stream().collect(Collectors.groupingBy(ReasoningChunk::getRealNodeId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<ReasoningChunk>> stringListEntry : r.entrySet()) {
            List<ReasoningChunk> value = stringListEntry.getValue();
            ReasoningChunk first = value.getFirst();
            String collect = value.stream().map(ReasoningChunk::getContent).collect(Collectors.joining());
            ReasoningContent content = new ReasoningContent();
            CommonUtils.copyProperties(first, content);
            content.setContent(collect);
            result.add(content);
        }
        return result;
    }
}
