package com.run.workflow.nodes.aichat;


import com.fasterxml.jackson.core.type.TypeReference;
import com.openai.core.JsonValue;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.run.RunApplication;
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
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.CHAT_WORKFLOW);

    @Override
    public List<Answer> getAnswerList(WorkFlowManage wm) {
        Answer answer = new Answer(this.getRealNodeId(), this.getNode().getId(), this.getDisplayId(),
                wm.getParams().get("conversationRecordId").toString(),
                wm.getParams().get("conversationId").toString());
        answer.put("content", this.context.getString("content"));
        answer.put("reasoning_content", this.context.getString("reasoning_content"));
        return List.of(answer);
    }

    public AIChat(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public AIChat(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, AIChat, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, AIChat node) {
            ChatCompletionAccumulator chatCompletionAccumulator = new ChatCompletionAccumulator(List.of("reasoning_content", "reasoning"));
            List<ConversationMessage> conversationMessages = new ArrayList<>((List<ConversationMessage>) workFlowManage.getContextVariable(List.of("start-node", "messages")));
            List<ChatCompletionMessageParam> messages = ConversationMessageConverter.toOpenAiMessages(conversationMessages);

            ModelMapper modelMapper = RunApplication.appComponent.modelMapper();
            modelMapper.getById(node.params.getModelId()).onSuccess(model -> {
                IProvider provider = ModelProvideConstants.valueOf(model.getProvider()).getProvider();
                String decrypt = RSAUtil.decrypt(model.getCredential());
                Map<String, Object> map = JacksonUtils.fromJson(decrypt, new TypeReference<Map<String, Object>>() {
                });
                Boolean stream = Optional.ofNullable(map.get("stream")).map(v -> (Boolean) v).orElse(true);
                ChatModel llm = provider.getModel(model.getModelType(), model.getModelName(), map, Map.of(), ChatModel.class);
                if (stream) {
                    String chunkId = CommonUtils.uuid7().toString();

                    llm.stream(messages, new JsonObject(map))
                            .subscribe(new AsyncStreamResponse.Handler<>() {
                                Boolean isReasoning = false;
                                Boolean reasoningEnd = false;

                                @Override
                                public void onNext(com.openai.models.chat.completions.ChatCompletionChunk chatCompletionChunk) {
                                    for (com.openai.models.chat.completions.ChatCompletionChunk.Choice choice : chatCompletionChunk.choices()) {
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
                    llm.invoke(messages, new JsonObject(map)).thenAcceptAsync(chatCompletion -> {
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
        return aiChatNodeData;
    }

    @Override
    public NodeResult<AIChat> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }


}
