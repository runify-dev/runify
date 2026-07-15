package com.run.workflow.nodes.aichat;


import com.fasterxml.jackson.core.type.TypeReference;
import com.run.RunApplication;
import com.run.ai.openai.AsyncStreamResponse;
import com.run.ai.openai.JsonValue;
import com.run.ai.openai.chat.ChatCompletionChunk;
import com.run.ai.openai.chat.ChatCompletionMessageParam;
import com.run.ai.openai.chat.ChatCompletionSystemMessageParam;
import com.run.ai.openai.chat.ChatCompletionUserMessageParam;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.pojo.File;
import com.run.common.util.ChatCompletionAccumulator;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.mapper.FileMapper;
import com.run.dao.mapper.ModelMapper;
import com.run.models.ChatModel;
import com.run.models.IProvider;
import com.run.models.ModelProvideConstants;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.converter.ConversationMessageConverter;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.*;
import com.run.workflow.nodes.aichat.entity.AIChatNodeData;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;

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
    static final Set<String> streamingFunctions = Set.of("run_command", "read_file", "apply_patch", "list_dir");

    private volatile boolean cancelled = false;
    private volatile AsyncStreamResponse<?> streamResponse;

    public AIChat(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public AIChat(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public int getPromptTokens() {
        JsonObject usage = this.context.getJsonObject("usage");
        if (usage != null) {
            return usage.getInteger("prompt_tokens");
        }
        return super.getPromptTokens();
    }

    @Override
    public int getCompletionTokens() {
        JsonObject usage = this.context.getJsonObject("usage");
        if (usage != null) {
            return usage.getInteger("completion_tokens");
        }
        return super.getPromptTokens();
    }

    @Override
    public void cancel() {
        if (cancelled) return;
        cancelled = true;
        super.cancel();
        AsyncStreamResponse<?> response = this.streamResponse;
        if (response != null) {
            response.cancel();
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, AIChat, Supplier<List<Node>>> {

        private static List<AIChatNodeData.Tool> getTools(WorkFlowManage workFlowManage, AIChatNodeData.ToolsConfig toolsConfig) {
            List<AIChatNodeData.Tool> result;
            if ("reference".equals(toolsConfig.getLocation())) {
                Object refValue = workFlowManage.getContextVariable(toolsConfig.getReference());
                result = JacksonUtils.convert(refValue, new TypeReference<>() {
                });
            } else {
                result = toolsConfig.getTools();
            }
            return CollectionUtils.isEmpty(result) ? List.of() : result;
        }

        /**
         * 构建 tools 列表
         *
         * @return tools 列表，如果为空返回 null
         */
        private static List<Map<String, Object>> buildToolsList(List<AIChatNodeData.Tool> tools) {

            List<Map<String, Object>> toolsList = new ArrayList<>();
            for (AIChatNodeData.Tool tool : tools) {
                Map<String, Object> toolMap = new HashMap<>();
                toolMap.put("type", tool.getType());

                if (tool.getFunction() != null) {
                    Map<String, Object> funcMap = new HashMap<>();
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

        /** 首条消息若是 SYSTEM 则返回其 JsonObject，用于把节点 system 合并进去（否则返回 null） */
        @SuppressWarnings("unchecked")
        private static JsonObject asSystemContent(Object first) {
            if (first == null) {
                return null;
            }
            JsonObject jo = first instanceof JsonObject j ? j
                    : (first instanceof Map<?, ?> m ? new JsonObject((Map<String, Object>) m) : null);
            return jo != null && "SYSTEM".equals(jo.getString("type")) ? jo : null;
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, AIChat node) {
            List<AIChatNodeData.Tool> tools = getTools(workFlowManage, node.params.getTools());
            ChatCompletionAccumulator chatCompletionAccumulator = new ChatCompletionAccumulator(List.of("reasoning_content", "reasoning"), tools.stream().map(t -> t.getFunction().getName()).toList());
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


            ArrayList<Object> _messages = new ArrayList<>(messages);
            if (StringUtils.isNotEmpty(node.params.getSystem())) {
                String system = workFlowManage.generatePrompt(node.params.getSystem());
                // 若上下文首条已是 SYSTEM（如压缩节点产出的工作便签），合并成一条：节点 system 在前、原 system 在后，
                // 避免 messages 里出现两条 system（Anthropic 等要求 system 唯一/顶层的模型会踩坑）。
                JsonObject firstSystem = asSystemContent(_messages.isEmpty() ? null : _messages.get(0));
                if (firstSystem != null) {
                    String existing = firstSystem.getString("content", "");
                    String merged = StringUtils.isBlank(existing) ? system : system + "\n\n" + existing;
                    _messages.set(0, JsonObject.mapFrom(new SystemContent(merged, "")));
                } else {
                    _messages.addFirst(JsonObject.mapFrom(new SystemContent(system, "")));
                }
            }
            QuestionContent questionContent = new QuestionContent("", List.of(), List.of(), List.of(), List.of(), "");

            if (StringUtils.isNotEmpty(node.params.getUser())) {
                String user = workFlowManage.generatePrompt(node.params.getUser());
                questionContent.setContent(user);
            }
            if (CollectionUtils.isNotEmpty(node.params.getImages())) {
                questionContent.setImages(resolveFiles(workFlowManage, node.params.getImages()));
            }
            if (CollectionUtils.isNotEmpty(node.params.getVideos())) {
                questionContent.setVideos(resolveFiles(workFlowManage, node.params.getVideos()));
            }
            if (StringUtils.isNotEmpty(node.params.getUser()) || CollectionUtils.isNotEmpty(node.params.getImages()) || CollectionUtils.isNotEmpty(node.params.getVideos())) {
                _messages.add(JsonObject.mapFrom(questionContent));
            }
            ModelMapper modelMapper = RunApplication.appComponent.modelMapper();
            FileMapper fileMapper = RunApplication.appComponent.fileMapper();
            Vertx vertx = RunApplication.appComponent.vertx();
            modelMapper.getById(node.params.getModelId()).compose(model -> {
                        if (model == null) {
                            return Future.failedFuture(new RuntimeException("模型不存在"));
                        }
                        return Future.succeededFuture(model);
                    }).onSuccess(model -> {

                        IProvider provider = ModelProvideConstants.valueOf(model.getProvider()).getProvider();
                        String decrypt = RSAUtil.decrypt(model.getCredential());
                        Map<String, Object> map = JacksonUtils.fromJson(decrypt, new TypeReference<Map<String, Object>>() {
                        });
                        // 添加 tools 配置
                        List<Map<String, Object>> toolsList = buildToolsList(tools);
                        if (toolsList != null) {
                            map.put("tools", toolsList);
                        }
                        JsonArray modelParameterForm = model.getModelParameterForm();
                        for (int i = 0; i < modelParameterForm.size(); i++) {
                            JsonObject jsonObject = modelParameterForm.getJsonObject(i);
                            map.put(jsonObject.getString("field"), jsonObject.getValue("defaultValue"));
                        }
                        Boolean stream = Optional.ofNullable(map.get("stream")).map(v -> (Boolean) v).orElse(true);
                        ChatModel llm = provider.getModel(model.getModelType(), model.getModelName(), map, Map.of(), ChatModel.class);
                        llm.transformation(_messages, fileMapper, vertx).thenCompose(ms -> {
                            if (stream) {
                                String chunkId = CommonUtils.uuid7().toString();
                                AsyncStreamResponse<ChatCompletionChunk> streamResp = llm.stream(ms, new JsonObject(map));
                                node.streamResponse = streamResp;
                                streamResp.subscribe(new AsyncStreamResponse.Handler<>() {
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
                                                workFlowManage.write(node, new TextContent(content, NodeStatus.RUNNING, node, (String) workFlowManage.getParams().get("workflowRunId"),
                                                        chunkId));
                                            });
                                        }
                                        chatCompletionAccumulator.onToolCallChunk(call -> {
                                            String id = call.getId();
                                            workFlowManage.write(node, new ToolCallContent(call.getFunctionName(), "", call.getFunctionArguments(), NodeStatus.RUNNING, node, (String) workFlowManage.getParams().get("workflowRunId"),
                                                    id));
                                        });
                                        chatCompletionAccumulator.append(chatCompletionChunk);
                                    }

                                    @Override
                                    public void onComplete(Optional<Throwable> error) {
                                        workFlowManage.write(node, new TextContent("", NodeStatus.SUCCESS, node, (String) workFlowManage.getParams().get("workflowRunId"),
                                                chunkId));
                                        if (node.cancelled) {
                                            return;
                                        }
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
                                            workFlowManage.writeContext(node, "usage", complete.getUsage().orElse(null));
                                            workFlowManage.writeContext(node, "toolCalls", complete.getToolCalls().stream().map(JsonObject::mapFrom).toList());
                                            workFlowManage.writeContext(node, "finishReason", complete.getFinishReason());
                                            workFlowManage.nextInvoke(node, () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList());

                                        }
                                    }

                                    @Override
                                    public void onCancel() {
                                        node.status = NodeStatus.CANCELLED;
                                        workFlowManage.writeContext(node, "finishReason", "cancelled");
                                        workFlowManage.assertionEnd();
                                    }
                                }).onCompleteFuture();
                            } else {
                                llm.invoke(ms, new JsonObject(map)).thenAcceptAsync(chatCompletion -> {
                                    if (node.cancelled) return;
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
                                    workFlowManage.nextInvoke(node, node.handleFail(workFlowManage, err));
                                    return null;
                                });
                            }
                            return CompletableFuture.completedFuture(null);
                        }).exceptionallyAsync(e -> {
                            return null;
                        });
                    })
                    .onFailure(e -> {
                        workFlowManage.nextInvoke(node, node.handleFail(workFlowManage, e));
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

        // 解析附件（图片/视频变量引用）
        aiChatNodeData.setImages(parseRefList(jsonObject.getJsonArray("images")));
        aiChatNodeData.setVideos(parseRefList(jsonObject.getJsonArray("videos")));

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

    /**
     * 解析 JsonArray<List<String>> 为 List<List<String>>
     * 前端格式: [["start-node", "images"], ["file-upload-node", "fileId"]]
     */
    private static List<List<String>> parseRefList(JsonArray array) {
        if (array == null) return List.of();
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonArray ref = array.getJsonArray(i);
            if (ref != null) {
                result.add(ref.stream().map(Object::toString).toList());
            }
        }
        return result;
    }

    /**
     * 解析变量引用列表为 File 对象列表
     * 每个引用 resolve 后可能是 String(url) 或 JsonObject({url, fileName, fileId, filePath})
     */
    private static List<File> resolveFiles(WorkFlowManage wfm, List<List<String>> refs) {
        List<File> files = new ArrayList<>();
        for (List<String> ref : refs) {
            if (ref == null || ref.isEmpty()) continue;
            Object val = wfm.getContextVariable(ref);
            if (val == null) continue;

            if (val instanceof JsonArray ja) {
                // 数组：逐个解析
                for (int i = 0; i < ja.size(); i++) {
                    toFile(ja.getValue(i)).ifPresent(files::add);
                }
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    toFile(item).ifPresent(files::add);
                }
            } else {
                toFile(val).ifPresent(files::add);
            }
        }
        return files;
    }

    private static Optional<File> toFile(Object val) {
        if (val instanceof JsonObject jo) {
            File file = new File();
            String url = jo.getString("url");
            if (StringUtils.isEmpty(url) && jo.getString("fileId") != null) {
                url = "./api/storage/file/" + jo.getString("fileId");
            }
            file.setUrl(url);
            file.setName(jo.getString("fileName"));
            return StringUtils.isNotEmpty(file.getUrl()) ? Optional.of(file) : Optional.empty();
        }
        if (val instanceof Map<?, ?> m) {
            File file = new File();
            String url = m.get("url") != null ? m.get("url").toString() : null;
            if (StringUtils.isEmpty(url) && m.get("fileId") != null) {
                url = "./api/storage/file/" + m.get("fileId");
            }
            file.setUrl(url);
            file.setName(m.get("fileName") != null ? m.get("fileName").toString() : null);
            return StringUtils.isNotEmpty(file.getUrl()) ? Optional.of(file) : Optional.empty();
        }
        if (val instanceof String s && StringUtils.isNotEmpty(s)) {
            File file = new File();
            file.setUrl(s);
            return Optional.of(file);
        }
        if (val != null) {
            File file = new File();
            file.setUrl(val.toString());
            return StringUtils.isNotEmpty(file.getUrl()) ? Optional.of(file) : Optional.empty();
        }
        return Optional.empty();
    }


}
