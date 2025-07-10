package com.run.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.POJONode;
import com.run.common.openai.request.completion_create_params.ChatCompletionToolParam;
import com.run.common.openai.request.completion_create_params.FunctionDefinition;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/24  20:15}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class JTest1 {
    public static void main(String[] args) throws JsonProcessingException {
        long l = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            test();
        }
        System.out.println(System.currentTimeMillis() - l);

    }

    public static void test() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonObject entries = new JsonObject();
        ChatCompletionToolParam chatCompletionToolParam = ChatCompletionToolParam.of(FunctionDefinition.of("get_weather", "获取指定日期和地点的天气信息。",
                Map.of("type", "object", "properties",
                        Map.of("date", Map.of("type", "string", "description", "获取天气的日期。"),
                                "location", Map.of("type", "string", "description", "获取天气的地点。")),
                        "required", List.of("date", "location"))

                , Boolean.TRUE));
        ChatCompletionToolParam chatCompletionToolParam1 = ChatCompletionToolParam.of(FunctionDefinition.of("get_weather_by_shanghai", "获取上海天气信息。",
                Map.of("type", "object", "properties",
                        Map.of("date", Map.of("type", "string", "description", "获取天气的日期。")),
                        "required", List.of("date", "location"))

                , Boolean.TRUE));
        POJONode jsonNodes = new POJONode(List.of(chatCompletionToolParam1, chatCompletionToolParam));

        entries.put("tools", List.of(chatCompletionToolParam1, chatCompletionToolParam));

        if (entries.containsKey("tools")) {

            List<ChatCompletionToolParam> chatCompletionToolParams = objectMapper.treeToValue(jsonNodes, new TypeReference<List<ChatCompletionToolParam>>() {
            });
        }
    }
}
