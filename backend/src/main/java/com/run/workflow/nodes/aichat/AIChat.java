package com.run.workflow.nodes.aichat;


import com.fasterxml.jackson.core.type.TypeReference;
import com.run.RunApplication;
import com.run.common.constants.MessageConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.openai.request.message.Message;
import com.run.common.openai.request.message.SystemMessage;
import com.run.common.openai.request.message.UserMessage;
import com.run.common.openai.response.ChatCompletion;
import com.run.common.openai.response.Choice;
import com.run.common.openai.response.chunk.ChatCompletionChunk;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.mapper.ModelMapper;
import com.run.models.BaseOpenaiChatModel;
import com.run.models.IProvider;
import com.run.models.ModelProvideConstants;
import com.run.models.callback.Callback;
import com.run.models.impl.openai.model.LLM;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.chunk.MessageChunk;
import com.run.workflow.message.struct.chunk.ReasoningChunk;
import com.run.workflow.message.struct.chunk.TextContentChunk;
import com.run.workflow.nodes.aichat.entity.AIChatNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import okhttp3.Call;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
            List<Message> messages = new ArrayList<>((List<Message>) workFlowManage.getContextVariable(List.of("start-node", "messages")));
            UserMessage userMessage = (UserMessage) messages.get(messages.size() - 1);
            if (StringUtils.isNotEmpty(node.params.getSystem())) {
                Optional<Message> systemMessage = messages.stream().filter(message -> message.getRole().equals("system")).findFirst();
                String systemContent = workFlowManage.generatePrompt(node.params.getSystem());
                if (systemMessage.isPresent()) {
                    ((SystemMessage) systemMessage.get()).setContent(systemContent);
                } else {
                    SystemMessage message = new SystemMessage();
                    message.setContent(systemContent);
                    messages.add(0, message);
                }
            }
            if (StringUtils.isNotEmpty(node.params.getUser())) {
                String user = workFlowManage.generatePrompt(node.params.getUser());
                userMessage.setContent(user);
            }
            ModelMapper modelMapper = RunApplication.appComponent.modelMapper();
            modelMapper.getById(node.params.getModelId()).onSuccess(model -> {
                IProvider provider = ModelProvideConstants.valueOf(model.getProvider()).getProvider();
                String decrypt = RSAUtil.decrypt(model.getCredential());
                Map<String, Object> map = JacksonUtils.fromJson(decrypt, new TypeReference<Map<String, Object>>() {
                });
                BaseOpenaiChatModel llm = provider.getModel(model.getModelType(), model.getModelName(), map, Map.of(), LLM.class);
                llm.invoke(messages, true, new Callback<>() {
                    private final StringBuilder reasoningContent = new StringBuilder();

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull ChatCompletion chatCompletion) {
                        for (Choice choice : chatCompletion.getChoices()) {
                            String content = choice.getMessage().getContent();
                            workFlowManage.write(node, new MessageChunk(MessageConstants.ASSISTANT, List.of(new TextContentChunk(content, node, (String) workFlowManage.getParams().get("workflowRunId")))));
                        }
                    }

                    @Override
                    public void onStream(@NotNull Call call, @NotNull ChatCompletionChunk chatCompletion) {
                        chatCompletion.getChoices().forEach(c -> {
                            String r = c.getDelta().getString("reasoning_content");
                            if (StringUtils.isNotEmpty(r)) {
                                reasoningContent.append(r);
                                workFlowManage.write(node, new MessageChunk(MessageConstants.ASSISTANT, List.of(new ReasoningChunk(r, node, (String) workFlowManage.getParams().get("workflowRunId")))));
                            }
                            String content = c.getDelta().getContent();
                            if (StringUtils.isNotEmpty(content)) {
                                workFlowManage.write(node, new MessageChunk(MessageConstants.ASSISTANT, List.of(new TextContentChunk(content, node, (String) workFlowManage.getParams().get("workflowRunId")))));
                            }
                        });
                    }

                    @Override
                    public void onFinish(@NotNull Call call) {
                        node.status = NodeStatus.SUCCESS;
                        workFlowManage.writeContext(node, "content", llm.getContent());
                        workFlowManage.writeContext(node, "reasoning_content", reasoningContent.toString());
                        workFlowManage.nextInvoke(node, () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList());
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {

                    }
                }, new JsonObject());
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
