package com.run.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.POJONode;
import com.run.common.openai.ChatCallback;
import com.run.common.openai.OpenAI;
import com.run.common.openai.request.completion_create_params.ChatCompletionToolParam;
import com.run.common.openai.request.message.Function;
import com.run.common.openai.request.message.Message;
import com.run.common.openai.request.message.UserMessage;
import com.run.common.openai.response.ChatCompletion;
import com.run.common.openai.response.ChatCompletionMessage;
import com.run.common.openai.response.ChatCompletionMessageToolCall;
import com.run.common.openai.response.CompletionUsage;
import com.run.common.openai.response.chunk.ChatCompletionChunk;
import com.run.common.openai.response.chunk.Choice;
import com.run.common.openai.response.chunk.ChoiceDeltaToolCall;
import com.run.common.openai.response.chunk.ChoiceDeltaToolCallFunction;
import com.run.models.callback.Callback;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import okhttp3.Call;
import okhttp3.Response;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/22  21:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public abstract class BaseOpenaiChatModel implements ChatModel {
    public static class BaseOpenaiCallBack implements ChatCallback {
        /**
         * 是否流式响应
         */
        protected boolean stream;
        /**
         * 结束标识
         */
        protected final StringBuilder finish_reason = new StringBuilder();
        /**
         * tokens
         */
        protected CompletionUsage completionUsage;
        /**
         * 工具响应
         */
        protected List<ChatCompletionMessageToolCall> toolCalls = new ArrayList<>();
        /**
         * ai助手响应
         */
        protected StringBuilder content = new StringBuilder();

        private Callback<ChatCompletion, ChatCompletionChunk> callback;

        public BaseOpenaiCallBack(boolean stream, Callback<ChatCompletion, ChatCompletionChunk> callback) {
            this.stream = stream;
            this.callback = callback;

        }

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            callback.onFailure(call, e);
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull String chunk) {
            if (chunk.equals("data: [DONE]")) {
                callback.onFinish(call);
                return;
            }
            String replace = chunk.replace("data:", "");
            try {
                if (this.stream) {
                    ChatCompletionChunk entries = objectMapper.readValue(replace, ChatCompletionChunk.class);
                    onStreamResponse(call, entries);
                } else {
                    ChatCompletion entries = objectMapper.readValue(replace, ChatCompletion.class);
                    onResponse(call, entries);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        private void onResponse(@NotNull Call call, @NotNull ChatCompletion chatCompletion) {
            for (com.run.common.openai.response.Choice choice : chatCompletion.getChoices()) {
                ChatCompletionMessage message = choice.getMessage();
                this.toolCalls = message.getTool_calls();
                String finishReason = choice.getFinish_reason();
                this.finish_reason.append(finishReason);
                this.content.append(message.getContent());
            }

            this.completionUsage = chatCompletion.getUsage();
            this.callback.onResponse(call, chatCompletion);
        }

        private void onStreamResponse(@NotNull Call call, @NotNull ChatCompletionChunk chunk) {
            for (Choice choice : chunk.getChoices()) {
                List<ChoiceDeltaToolCall> tool_calls = choice.getDelta().getTool_calls();
                if (CollectionUtils.isNotEmpty(tool_calls)) {
                    for (ChoiceDeltaToolCall tool_call : tool_calls) {
                        int index = tool_call.getIndex();
                        ChatCompletionMessageToolCall toolCall = toolCalls.get(index);
                        ChoiceDeltaToolCallFunction function = tool_call.getFunction();
                        if (toolCall != null) {
                            Function oldFunction = toolCall.getFunction();
                            oldFunction.setName(oldFunction.getName() + function.getName());
                            oldFunction.setArguments(oldFunction.getArguments() + function.getArguments());
                        } else {
                            ChatCompletionMessageToolCall c = new ChatCompletionMessageToolCall();
                            try {
                                BeanUtils.copyProperties(tool_call, c);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                            toolCalls.add(index, c);
                        }
                    }
                }

                String finishReason = choice.getFinish_reason();
                if (StringUtils.isNotEmpty(finishReason)) {
                    this.finish_reason.append(finishReason);
                }

            }
            this.completionUsage = chunk.getUsage();
            this.callback.onStream(call, chunk);

        }
    }

    /**
     * url
     */
    protected String baseUrl;
    /**
     * key
     */
    protected String apiKey;
    /**
     * 模型名称
     */
    protected String model;
    /**
     * 调用客户端
     */
    private OpenAI client;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private BaseOpenaiCallBack baseOpenaiCallBack;

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public BaseOpenaiChatModel(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.client = new OpenAI(apiKey, baseUrl, model);
    }

    @SneakyThrows
    private ChatCompletionToolParam toTool(Object o) {
        if (o instanceof ChatCompletionToolParam) {
            return (ChatCompletionToolParam) o;
        } else if (o instanceof JsonObject) {
            return objectMapper.treeToValue(new POJONode(o), ChatCompletionToolParam.class);
        } else if (o instanceof Map<?, ?>) {
            return objectMapper.treeToValue(new POJONode(o), ChatCompletionToolParam.class);
        } else if (o instanceof String) {
            return objectMapper.readValue((String) o, ChatCompletionToolParam.class);
        }
        throw new RuntimeException("不支持的数据类型");
    }

    private List<ChatCompletionToolParam> getTools(JsonObject other) {
        if (other.containsKey("tools")) {
            return other.getJsonArray("tools").stream().map(this::toTool).toList();
        }
        return null;
    }

    @Override
    public void validate(String modelType, String modelName, Map<String, Object> modelCredential, Map<String, Object> other) {
        Boolean stream = (Boolean) other.getOrDefault("stream", false);
        try {
            Response response = this.client.invokeBlock(List.of(new UserMessage("你好")), stream, null, null, new JsonObject(other));
            if (!response.isSuccessful()) {
                throw new RuntimeException("模型参数错误" + response.body());
            }
            response.close();
        } catch (IOException e) {
            throw new RuntimeException("模型参数错误" + e);
        }
    }

    @Override
    public void invoke(List<Message> messages, boolean stream, Callback<ChatCompletion, ChatCompletionChunk> callback, JsonObject other) {
        List<ChatCompletionToolParam> tools = getTools(other);
        String tool_choice = other.getString("tool_choice");
        baseOpenaiCallBack = new BaseOpenaiCallBack(stream, callback);
        this.client.invoke(messages, true, tools, tool_choice, baseOpenaiCallBack, other);
    }

    @Override
    public CompletionUsage getCompletionUsage() {
        return baseOpenaiCallBack.completionUsage;
    }

    @Override
    public String getFinishReason() {
        return baseOpenaiCallBack.finish_reason.toString();
    }

    @Override
    public List<ChatCompletionMessageToolCall> getChoiceDeltaToolCall() {
        return baseOpenaiCallBack.toolCalls;
    }
}
