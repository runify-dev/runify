package com.run.workflow.message.aggregator;

import com.run.workflow.message.struct.Content;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  14:55}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class AggregationManager {
    private final Map<String, Integer> keyToIndex = new HashMap<>();  // 关键优化
    @Getter
    private final List<Content> contents = new ArrayList<>();

    public void aggregate(Content chunk) {
        String key = chunk.getId() + "_" + chunk.getType();

        Integer idx = keyToIndex.get(key);
        if (idx == null) {
            // 新key
            keyToIndex.put(key, contents.size());
            contents.add(chunk);
        } else {
            // 已存在，聚合
            Content prev = contents.get(idx);
            ContentAggregator<Content> aggregator =
                    (ContentAggregator<Content>) AggregatorFactory.getAggregator(prev.getClass());
            contents.set(idx, aggregator.apply(prev, chunk));
        }
    }
}