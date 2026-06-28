package com.run.integrations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.common.util.JacksonUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 集成供应商通用 HTTP/JSON 原语 }
 */
public class Http {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public static Map<String, Object> getJson(String url, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15)).GET();
        if (bearer != null) {
            b.header("Authorization", "Bearer " + bearer);
        }
        HttpResponse<String> resp = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return parse(resp.body());
    }

    public static Map<String, Object> postJson(String url, String json, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (bearer != null) {
            b.header("Authorization", "Bearer " + bearer);
        }
        HttpResponse<String> resp = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return parse(resp.body());
    }

    private static Map<String, Object> parse(String body) {
        return JacksonUtils.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
    }

    public static Integer asInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return o == null ? null : Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
