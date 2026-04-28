package com.run.ai.openai.chat;

import com.run.ai.openai.OpenAiException;
import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

final class ChatCompletionHttpSupport {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private ChatCompletionHttpSupport() {
    }

    @SuppressWarnings("deprecation")
    static Request buildRequest(String baseUrl,
                                String apiKey,
                                ChatCompletionCreateParams params,
                                boolean stream) {
        JsonObject body = params.toJsonObject(stream);
        RequestBody requestBody = RequestBody.create(JSON, body.encode());

        Request.Builder builder = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Accept", stream ? "text/event-stream" : "application/json");

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        return builder.build();
    }

    static String readBodyOrThrow(Response response) throws IOException {
        ResponseBody body = response.body();
        String text = body == null ? "" : body.string();
        if (!response.isSuccessful()) {
            throw new OpenAiException("OpenAI request failed: HTTP " + response.code() + ", body=" + text);
        }
        if (text == null || text.isBlank()) {
            throw new OpenAiException("OpenAI response body is empty");
        }
        return text;
    }
}
