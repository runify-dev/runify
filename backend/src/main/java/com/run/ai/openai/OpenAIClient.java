package com.run.ai.openai;

import com.run.ai.openai.chat.AsyncChatCompletionService;
import com.run.ai.openai.chat.ChatCompletionService;

/**
 * Small OpenAI-compatible client surface used by this project.
 */
public interface OpenAIClient {

    ChatResource chat();

    AsyncOpenAIClient async();

    interface ChatResource {
        ChatCompletionService completions();
    }

    interface AsyncOpenAIClient {
        AsyncChatResource chat();
    }

    interface AsyncChatResource {
        AsyncChatCompletionService completions();
    }
}
