package com.run.workflow.message.aggregator;

import com.run.workflow.message.aggregator.impl.FailureAggregator;
import com.run.workflow.message.aggregator.impl.ReasoningAggregator;
import com.run.workflow.message.aggregator.impl.TextAggregator;
import com.run.workflow.message.aggregator.impl.ToolCallAggregator;
import com.run.workflow.message.struct.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  14:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class AggregatorFactory {
    private static final Map<Class<? extends Content>, ContentAggregator<?>> AGGREGATORS = new HashMap<>();

    static {
        AGGREGATORS.put(TextContent.class, new TextAggregator());
        AGGREGATORS.put(ReasoningContent.class, new ReasoningAggregator());
        AGGREGATORS.put(FailureContent.class, new FailureAggregator());
        AGGREGATORS.put(ToolCallContent.class, new ToolCallAggregator());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Content> ContentAggregator<T> getAggregator(Class<T> clazz) {
        ContentAggregator<?> aggregator = AGGREGATORS.get(clazz);
        if (aggregator == null) {
            throw new IllegalArgumentException("No aggregator found for class: " + clazz);
        }
        return (ContentAggregator<T>) aggregator;
    }

    public static <T extends Content> Optional<ContentAggregator<T>> getAggregatorOptional(Class<T> clazz) {
        ContentAggregator<?> aggregator = AGGREGATORS.get(clazz);
        return Optional.ofNullable((ContentAggregator<T>) aggregator);
    }
}
