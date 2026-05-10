package com.run.workflow.nodes.aichat;


import com.fasterxml.jackson.core.type.TypeReference;
import com.run.RunApplication;
import com.run.ai.openai.AsyncStreamResponse;
import com.run.ai.openai.JsonValue;
import com.run.ai.openai.chat.ChatCompletionChunk;
import com.run.ai.openai.chat.ChatCompletionMessageParam;
import com.run.ai.openai.chat.ChatCompletionUserMessageParam;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.ChatCompletionAccumulator;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.entity.ConversationMessage;
import com.run.dao.mapper.ModelMapper;
import com.run.models.ChatModel;
import com.run.models.IProvider;
import com.run.models.ModelProvideConstants;
import com.run.workflow.*;
import com.run.workflow.converter.ConversationMessageConverter;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ReasoningContent;
import com.run.workflow.message.struct.TextContent;
import com.run.workflow.nodes.aichat.entity.AIChatNodeData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/20  21:43}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class AIChat extends INode<AIChat, AIChatNodeData> {
    /**
     * 节点类型
     */
    public final static String type = "ai-chat-node";
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.CHAT_WORKFLOW, WorkflowType.CHAT_WORKFLOW_LOOP);

    public AIChat(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public AIChat(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, AIChat, Supplier<List<Node>>> {

        /**
         * 构建 tools 列表
         *
         * @param toolsConfig 工具配置
         * @return tools 列表，如果为空返回 null
         */
        private static List<Map<String, Object>> buildToolsList(AIChatNodeData.ToolsConfig toolsConfig) {
            if (toolsConfig == null || !"customize".equals(toolsConfig.getLocation()) || toolsConfig.getTools() == null) {
                return null;
            }

            List<Map<String, Object>> toolsList = new ArrayList<>();
            for (AIChatNodeData.Tool tool : toolsConfig.getTools()) {
                Map<String, Object> toolMap = new java.util.HashMap<>();
                toolMap.put("type", tool.getType());

                if (tool.getFunction() != null) {
                    Map<String, Object> funcMap = new java.util.HashMap<>();
                    funcMap.put("name", tool.getFunction().getName());
                    funcMap.put("description", tool.getFunction().getDescription());

                    if (tool.getFunction().getParameters() != null) {
                        funcMap.put("parameters", tool.getFunction().getParameters());
                    }

                    toolMap.put("function", funcMap);
                }

                toolsList.add(toolMap);
            }

            return toolsList.isEmpty() ? null : toolsList;
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, AIChat node) {
            ChatCompletionAccumulator chatCompletionAccumulator = new ChatCompletionAccumulator(List.of("reasoning_content", "reasoning"));

            // 获取历史消息
            List<Object> messages;
            AIChatNodeData.ContextConfig contextConfig = node.params.getContextConfig();

            if (contextConfig != null && Boolean.TRUE.equals(contextConfig.getEnableContext())
                    && contextConfig.getContextVariable() != null && !contextConfig.getContextVariable().isEmpty()) {
                // 自定义上下文：从指定变量获取
                Object contextObj = workFlowManage.getContextVariable(contextConfig.getContextVariable());
                messages = contextObj instanceof List ? (List<Object>) contextObj : new ArrayList<>();
            } else {
                // 默认：从 start-node 获取
                Object startMessages = workFlowManage.getContextVariable(List.of("start-node", "messages"));
                messages = startMessages instanceof List ? (List<Object>) startMessages : new ArrayList<>();
            }

            // 应用轮次限制
            if (contextConfig != null && contextConfig.getContextNumber() != null && contextConfig.getContextNumber() > 0) {
                int limit = contextConfig.getContextNumber() * 2; // 每轮包含 user + assistant
                if (messages.size() > limit) {
                    messages = messages.subList(messages.size() - limit, messages.size()
                    );
                }
            }
            List<Object> messages1 = messages;

            List<ChatCompletionMessageParam> _messages = ConversationMessageConverter.toOpenAiMessage(messages);
            if (StringUtils.isNotEmpty(node.params.getSystem())) {
                String system = workFlowManage.generatePrompt(node.params.getSystem());
                _messages.addFirst(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content(system).build()));
            }
            if (StringUtils.isNotEmpty(node.params.getUser())) {
                String user = workFlowManage.generatePrompt(node.params.getUser());
                _messages.add(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content(user).build()));
            }
            ModelMapper modelMapper = RunApplication.appComponent.modelMapper();
            modelMapper.getById(node.params.getModelId()).onSuccess(model -> {
                IProvider provider = ModelProvideConstants.valueOf(model.getProvider()).getProvider();
                String decrypt = RSAUtil.decrypt(model.getCredential());
                Map<String, Object> map = JacksonUtils.fromJson(decrypt, new TypeReference<Map<String, Object>>() {
                });

                // 添加 tools 配置
                List<Map<String, Object>> toolsList = buildToolsList(node.params.getTools());
                if (toolsList != null) {
                    map.put("tools", toolsList);
                }
                JsonArray modelParameterForm = model.getModelParameterForm();
                for (int i = 0; i < modelParameterForm.size(); i++) {
                    JsonObject jsonObject = modelParameterForm.getJsonObject(i);
                    map.put("max_tokens", jsonObject.getValue("defaultValue"));
                }
                Boolean stream = Optional.ofNullable(map.get("stream")).map(v -> (Boolean) v).orElse(true);
                ChatModel llm = provider.getModel(model.getModelType(), model.getModelName(), map, Map.of(), ChatModel.class);
                if (stream) {
                    String chunkId = CommonUtils.uuid7().toString();

                    llm.stream(_messages, new JsonObject(map))
                            .subscribe(new AsyncStreamResponse.Handler<>() {
                                Boolean isReasoning = false;
                                Boolean reasoningEnd = false;

                                @Override
                                public void onNext(ChatCompletionChunk chatCompletionChunk) {
                                    for (ChatCompletionChunk.Choice choice : chatCompletionChunk.choices()) {
                                        JsonValue reasoningContent = choice.delta()._additionalProperties().get("reasoning_content");
                                        if (reasoningContent != null && !reasoningEnd) {
                                            isReasoning = true;
                                            String reasoning = reasoningContent.convert(String.class);
                                            if (StringUtils.isNotEmpty(reasoning)) {
                                                workFlowManage.write(node, new ReasoningContent(reasoning, NodeStatus.RUNNING, node, (String) workFlowManage.getParams().get("workflowRunId"), chunkId));
                                            }
                                        }
                                        choice.delta().content().ifPresent(content -> {
                                            if (isReasoning && !reasoningEnd) {
                                                reasoningEnd = true;
                                                workFlowManage.write(node, new ReasoningContent("", NodeStatus.SUCCESS, node, (String) workFlowManage.getParams().get("workflowRunId"), chunkId));
                                            }
                                            workFlowManage.write(node, new TextContent(content, node, (String) workFlowManage.getParams().get("workflowRunId"),
                                                    chunkId));
                                        });

                                    }
                                    chatCompletionAccumulator.append(chatCompletionChunk);
                                }

                                @Override
                                public void onComplete(Optional<Throwable> error) {
                                    if (error.isPresent()) {
                                        node.status = NodeStatus.FAIL;
                                        workFlowManage.writeContext(node, "finishReason", "error");
                                        workFlowManage.nextFailInvoke(node, error.get());
                                    } else {
                                        node.status = NodeStatus.SUCCESS;
                                        ChatCompletionAccumulator.AccumulatedResult complete = chatCompletionAccumulator.complete();
                                        workFlowManage.writeContext(node, "content", complete.getContent());
                                        workFlowManage.writeContext(node, "reasoningContent", complete.getAdditionalProperty("reasoning_content").orElse(null));
                                        workFlowManage.writeContext(node, "refusal", complete.getRefusal());
                                        workFlowManage.writeContext(node, "isRefusal", complete.isRefusal());
                                        workFlowManage.writeContext(node, "toolCalls", complete.getToolCalls());
                                        workFlowManage.writeContext(node, "finishReason", complete.getFinishReason());
                                        workFlowManage.nextInvoke(node, () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList());

                                    }
                                }
                            }).onCompleteFuture();
                } else {
                    llm.invoke(_messages, new JsonObject(map)).thenAcceptAsync(chatCompletion -> {
                        chatCompletionAccumulator.append(chatCompletion);
                        node.status = NodeStatus.SUCCESS;
                        ChatCompletionAccumulator.AccumulatedResult complete = chatCompletionAccumulator.complete();
                        workFlowManage.writeContext(node, "content", complete.getContent());
                        workFlowManage.writeContext(node, "reasoningContent", complete.getAdditionalProperty("reasoning_content").orElse(null));
                        workFlowManage.writeContext(node, "refusal", complete.getRefusal());
                        workFlowManage.writeContext(node, "isRefusal", complete.isRefusal());
                        workFlowManage.writeContext(node, "toolCalls", complete.getToolCalls());
                        workFlowManage.writeContext(node, "finishReason", complete.getFinishReason());
                        workFlowManage.nextInvoke(node, () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList());

                    }).exceptionallyAsync(err -> {
                        node.status = NodeStatus.FAIL;
                        return null;
                    });
                }
            });
            return null;
        }
    }

    @Override
    public AIChatNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        AIChatNodeData aiChatNodeData = new AIChatNodeData();
        aiChatNodeData.setModelId(jsonObject.getString("modelId"));
        aiChatNodeData.setUser(jsonObject.getString("user"));
        aiChatNodeData.setSystem(jsonObject.getString("system"));

        // 解析 contextConfig 配置
        AIChatNodeData.ContextConfig contextConfig = new AIChatNodeData.ContextConfig();
        contextConfig.setEnableContext(jsonObject.getBoolean("enableContext", false));
        if (jsonObject.getJsonArray("contextVariable") != null) {
            contextConfig.setContextVariable(jsonObject.getJsonArray("contextVariable").stream()
                    .map(Object::toString)
                    .toList());
        }
        contextConfig.setContextNumber(jsonObject.getInteger("contextNumber"));
        aiChatNodeData.setContextConfig(contextConfig);

        // 解析 tools 配置
        JsonObject toolsObj = jsonObject.getJsonObject("tools");
        if (toolsObj != null) {
            AIChatNodeData.ToolsConfig toolsConfig = new AIChatNodeData.ToolsConfig();
            toolsConfig.setLocation(toolsObj.getString("location"));
            toolsConfig.setReference(toolsObj.getJsonArray("reference") != null ?
                    toolsObj.getJsonArray("reference").stream().map(Object::toString).toList() : null);

            if ("customize".equals(toolsConfig.getLocation())) {
                JsonArray toolsArray = toolsObj.getJsonArray("tools");
                if (toolsArray != null) {
                    List<AIChatNodeData.Tool> tools = new ArrayList<>();
                    for (int i = 0; i < toolsArray.size(); i++) {
                        JsonObject toolObj = toolsArray.getJsonObject(i);
                        AIChatNodeData.Tool tool = new AIChatNodeData.Tool();
                        tool.setType(toolObj.getString("type"));

                        JsonObject funcObj = toolObj.getJsonObject("function");
                        if (funcObj != null) {
                            AIChatNodeData.Function func = new AIChatNodeData.Function();
                            func.setName(funcObj.getString("name"));
                            func.setDescription(funcObj.getString("description"));
                            func.setParameters(funcObj.getJsonObject("parameters") != null ?
                                    funcObj.getJsonObject("parameters").getMap() : null);
                            tool.setFunction(func);
                        }
                        tools.add(tool);
                    }
                    toolsConfig.setTools(tools);
                }
            }
            aiChatNodeData.setTools(toolsConfig);
        }

        return aiChatNodeData;
    }

    @Override
    public NodeResult<AIChat> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }


}
