package com.run.workflow.message.impl;

import com.run.common.constants.MessageConstants;
import com.run.common.openai.request.message.AssistantMessage;
import com.run.common.openai.request.message.ToolMessage;
import com.run.common.openai.request.message.UserMessage;
import com.run.dao.entity.ConversationMessage;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageImpl {


    public static List<Message> toMessage(List<ConversationMessage> conversationMessage) {
        return conversationMessage.stream().map(MessageImpl::toMessage).toList();
    }

    public static Message toMessage(ConversationMessage message) {
        JsonArray array = message.getContent();
        List<Content> contents = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject jsonObject = array.getJsonObject(i);
            Content content = ContentImpl.newInstance(jsonObject);
            contents.add(content);
        }
        return new Message(message.getType(), contents);
    }

    public static com.run.common.openai.request.message.Message toChatMessage(ConversationMessage conversationMessage) {
        Message message = toMessage(conversationMessage);
        String content = message.content().stream().map(Content::toString).collect(Collectors.joining());
        if (message.type().equals(MessageConstants.USER)) {
            return new UserMessage(content);
        } else if (message.type().equals(MessageConstants.ASSISTANT)) {
            AssistantMessage assistantMessage = new AssistantMessage();
            assistantMessage.setContent(content);
            return assistantMessage;
        } else if (message.type().equals(MessageConstants.TOOL)) {
            ToolMessage toolMessage = new ToolMessage();
            toolMessage.setContent(content);
            return toolMessage;
        }
        return null;
    }

    public static List<com.run.common.openai.request.message.Message> toChatMessage(List<ConversationMessage> conversationMessage) {
        return conversationMessage.stream().map(MessageImpl::toChatMessage).toList();
    }


}
