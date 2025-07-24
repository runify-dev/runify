package com.run.workflow.nodes.AIChat;


import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.openai.request.message.Message;
import com.run.common.openai.request.message.SystemMessage;
import com.run.common.openai.response.ChatCompletion;
import com.run.common.openai.response.chunk.ChatCompletionChunk;
import com.run.dao.entity.Model;
import com.run.models.IProvider;
import com.run.models.ModelProvideConstants;
import com.run.models.callback.Callback;
import com.run.models.impl.openai.model.LLM;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.AIChat.entity.AIChatNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import okhttp3.Call;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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

    public AIChat(Node node, JsonObject params, List<String> upNodeIdList, String salt) {
        super(node, params, upNodeIdList, salt);
    }

    public AIChat(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator) {
        super(node, params, upNodeIdList, salt, context, validator);
    }

    public static class Handle implements BiFunction<WorkFlowManage, AIChat, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, AIChat node) {
            List<Message> messages = (List<Message>) workFlowManage.getContextVariable(List.of("start-node", "messages"));
            Message userMessage = messages.get(messages.size() - 1);
            if (StringUtils.isNotEmpty(node.params.getSystem())) {
                Optional<Message> systemMessage = messages.stream().filter(message -> message.getRole().equals("system")).findFirst();
                String systemContent = workFlowManage.generatePrompt(node.params.getSystem());
                if (systemMessage.isPresent()) {
                    systemMessage.get().setContent(systemContent);
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
            Model model = new Model("1", "测试模型", "描述", "openai_provider", "LLM", "deepseek-r1", Map.of("baseUrl", "https://qianfan.baidubce.com/v2/chat/completions"
                    , "apiKey", "xx"), List.of(), Map.of());
            IProvider provider = ModelProvideConstants.valueOf(model.getProvider()).getProvider();
            LLM llm = provider.getModel(model.getModelType(), model.getModelName(), model.getCredential(), Map.of(), LLM.class);
            llm.invoke(messages, true, new Callback<>() {
                @Override
                public void onResponse(@NotNull Call call, @NotNull ChatCompletion chatCompletion) {
                    workFlowManage.write(chatCompletion.toChunk());
                }

                @Override
                public void onStream(@NotNull Call call, @NotNull ChatCompletionChunk chatCompletion) {
                    workFlowManage.write(chatCompletion);
                }

                @Override
                public void onFinish(@NotNull Call call) {
                    node.status = NodeStatus.SUCCESS;
                    workFlowManage.nextInvoke(node, () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList());
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {

                }
            }, new JsonObject());
            return null;
        }
    }

    @Override
    public AIChatNodeData getNodeData(JsonObject params) {
        String modelId = params.getString("modelId");
        String system = params.getString("system");
        String user = params.getString("user");
        AIChatNodeData aiChatNodeData = new AIChatNodeData();
        aiChatNodeData.setModelId(modelId);
        aiChatNodeData.setUser(user);
        aiChatNodeData.setSystem(system);
        return aiChatNodeData;
    }

    @Override
    public NodeResult<AIChat> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }


}
