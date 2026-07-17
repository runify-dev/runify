package com.run.handler.model.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.ai.openai.AsyncStreamResponse;
import com.run.ai.openai.JsonValue;
import com.run.ai.openai.chat.ChatCompletionChunk;
import com.run.ai.openai.chat.ChatCompletionMessageParam;
import com.run.common.result.Result;
import com.run.common.util.ChatCompletionAccumulator;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.mapper.ModelMapper;
import com.run.handler.model.IModelChatCompletionHandler;
import com.run.models.ChatModel;
import com.run.models.IProvider;
import com.run.models.ModelProvideConstants;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/7/15}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Slf4j
public class ModelChatCompletionHandlerImpl implements IModelChatCompletionHandler {

    /**
     * messages 条数护栏，迭代上限由前端 agent loop 控制
     */
    private static final int MAX_MESSAGES = 200;

    private final ModelMapper modelMapper;

    @Inject
    public ModelChatCompletionHandlerImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public void completion(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.end(Result.error(400, "请求体不能为空").toBuffer());
            return;
        }
        JsonArray messages = body.getJsonArray("messages", new JsonArray());
        JsonArray tools = body.getJsonArray("tools");
        if (messages.isEmpty()) {
            context.end(Result.error(400, "messages 不能为空").toBuffer());
            return;
        }
        if (messages.size() > MAX_MESSAGES) {
            context.end(Result.error(400, "messages 超出条数上限").toBuffer());
            return;
        }
        modelMapper.getById(resourceId).onSuccess(model -> {
            if (model == null) {
                context.end(Result.error(500, "模型不存在").toBuffer());
                return;
            }
            try {
                IProvider provider = ModelProvideConstants.valueOf(model.getProvider()).getProvider();
                String decrypt = RSAUtil.decrypt(model.getCredential());
                Map<String, Object> map = JacksonUtils.fromJson(decrypt, new TypeReference<Map<String, Object>>() {
                });
                JsonArray modelParameterForm = model.getModelParameterForm();
                for (int i = 0; i < modelParameterForm.size(); i++) {
                    JsonObject jsonObject = modelParameterForm.getJsonObject(i);
                    map.put(jsonObject.getString("field"), jsonObject.getValue("defaultValue"));
                }
                List<String> toolNames = getToolNames(tools);
                if (tools != null && !tools.isEmpty()) {
                    map.put("tools", tools.getList());
                }
                map.put("stream", true);
                ChatModel llm = provider.getModel(model.getModelType(), model.getModelName(), map, Map.of(), ChatModel.class);
                List<ChatCompletionMessageParam> ms = new ArrayList<>();
                for (int i = 0; i < messages.size(); i++) {
                    ms.add(ChatCompletionMessageParam.fromJsonObject(messages.getJsonObject(i)));
                }
                stream(context, llm, ms, new JsonObject(map), toolNames);
            } catch (Exception e) {
                log.error("chat-completion 调用失败:", e);
                context.end(Result.error(500, e.getMessage()).toBuffer());
            }
        }).onFailure(e -> context.end(Result.error(500, e.getMessage()).toBuffer()));
    }

    private void stream(RoutingContext context,
                        ChatModel llm,
                        List<ChatCompletionMessageParam> ms,
                        JsonObject extra,
                        List<String> toolNames) {
        context.response()
                .setChunked(true)
                .putHeader("Content-Type", "text/event-stream;charset=utf-8")
                .putHeader("Cache-Control", "no-cache");

        ChatCompletionAccumulator accumulator = new ChatCompletionAccumulator(List.of("reasoning_content", "reasoning"), toolNames);
        // 每个 tool_call 只下发一次 start 事件，参数由服务端累积后随 completion 整体下发
        Set<String> startedToolCalls = new HashSet<>();
        accumulator.onToolCallChunk(call -> {
            if (call.getId() != null && startedToolCalls.add(call.getId())) {
                writeEvent(context, new JsonObject()
                        .put("type", "tool_call_start")
                        .put("id", call.getId())
                        .put("name", call.getFunctionName()));
            }
        });

        AsyncStreamResponse<ChatCompletionChunk> streamResponse = llm.stream(ms, extra);
        context.response().closeHandler(v -> streamResponse.cancel());

        streamResponse.subscribe(new AsyncStreamResponse.Handler<>() {
            @Override
            public void onNext(ChatCompletionChunk chunk) {
                for (ChatCompletionChunk.Choice choice : chunk.choices()) {
                    JsonValue reasoningContent = choice.delta()._additionalProperties().get("reasoning_content");
                    if (reasoningContent == null) {
                        reasoningContent = choice.delta()._additionalProperties().get("reasoning");
                    }
                    if (reasoningContent != null) {
                        String reasoning = reasoningContent.convert(String.class);
                        if (reasoning != null && !reasoning.isEmpty()) {
                            writeEvent(context, new JsonObject().put("type", "reasoning").put("content", reasoning));
                        }
                    }
                    choice.delta().content().ifPresent(content -> {
                        if (!content.isEmpty()) {
                            writeEvent(context, new JsonObject().put("type", "delta").put("content", content));
                        }
                    });
                }
                accumulator.append(chunk);
            }

            @Override
            public void onComplete(Optional<Throwable> error) {
                if (context.response().closed()) {
                    return;
                }
                if (error.isPresent()) {
                    writeEvent(context, new JsonObject().put("type", "error").put("message", error.get().getMessage()));
                } else {
                    ChatCompletionAccumulator.AccumulatedResult result = accumulator.complete();
                    JsonArray toolCalls = new JsonArray();
                    for (ChatCompletionAccumulator.AccumulatedToolCall call : result.getToolCalls()) {
                        toolCalls.add(new JsonObject()
                                .put("id", call.getId())
                                .put("type", call.getType() == null ? "function" : call.getType())
                                .put("function", new JsonObject()
                                        .put("name", call.getFunctionName())
                                        .put("arguments", call.getFunctionArguments())));
                    }
                    // thinking 模型要求把思考内容按原字段名回传（如 reasoning_content），
                    // 一并下发给前端，由前端拼回 assistant 消息
                    String reasoningKey = null;
                    String reasoningContent = null;
                    for (String key : List.of("reasoning_content", "reasoning")) {
                        Optional<String> value = result.getAdditionalProperty(key).filter(s -> !s.isBlank());
                        if (value.isPresent()) {
                            reasoningKey = key;
                            reasoningContent = value.get();
                            break;
                        }
                    }
                    writeEvent(context, new JsonObject()
                            .put("type", "completion")
                            .put("content", result.getContent())
                            .put("reasoningKey", reasoningKey)
                            .put("reasoningContent", reasoningContent)
                            .put("toolCalls", toolCalls)
                            .put("finishReason", result.getFinishReason())
                            .put("usage", result.getUsage().orElse(null)));
                }
                context.response().write("data: [DONE]\n\n");
                context.response().end();
            }
        });
    }

    private void writeEvent(RoutingContext context, JsonObject event) {
        if (!context.response().closed()) {
            context.response().write("data: " + event.encode() + "\n\n");
        }
    }

    private List<String> getToolNames(JsonArray tools) {
        if (tools == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < tools.size(); i++) {
            JsonObject function = tools.getJsonObject(i).getJsonObject("function");
            if (function != null && function.getString("name") != null) {
                names.add(function.getString("name"));
            }
        }
        return names;
    }
}
