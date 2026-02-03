package com.run.workflow.message.impl.impl;


import com.run.common.constants.ContentTypeConstants;
import com.run.common.util.CommonUtils;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.JsonContent;
import com.run.workflow.message.struct.chunk.JsonContentChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonChunkListImpl {
    public static Boolean support(ContentTypeConstants type) {
        return type == ContentTypeConstants.JSON;
    }

    public static List<Content> toBlock(List<JsonContentChunk> self) {
        List<Content> result = new ArrayList<>();
        LinkedHashMap<String, List<JsonContentChunk>> r = self.stream().collect(Collectors.groupingBy(JsonContentChunk::getRealNodeId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<JsonContentChunk>> stringListEntry : r.entrySet()) {
            List<JsonContentChunk> value = stringListEntry.getValue();
            JsonContentChunk first = value.getFirst();
            String collect = value.stream().map(JsonContentChunk::getContent).collect(Collectors.joining());
            JsonContent jsonContent = new JsonContent();
            CommonUtils.copyProperties(first, jsonContent);
            jsonContent.setContent(collect);
            result.add(jsonContent);
        }
        return result;
    }
}
