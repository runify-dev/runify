package com.run.integrations.wecom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.common.util.JacksonUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信底层 HTTP/编码原语(应用/机器人共用), 不含任何业务逻辑 }
 */
public class WecomHttp {

    public static final String BASE = "https://qyapi.weixin.qq.com/cgi-bin";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public static Map<String, Object> getJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return JacksonUtils.fromJson(resp.body(), new TypeReference<Map<String, Object>>() {
        });
    }

    public static Map<String, Object> postJson(String url, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return JacksonUtils.fromJson(resp.body(), new TypeReference<Map<String, Object>>() {
        });
    }

    public static byte[] downloadBytes(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30)).GET().build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
    }

    /**
     * 下载素材, 若返回 JSON(表示错误)则抛出
     */
    public static byte[] downloadMedia(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        String ct = resp.headers().firstValue("Content-Type").orElse("");
        if (ct.contains("application/json")) {
            throw new RuntimeException("wecom media/get failed: " + new String(resp.body(), StandardCharsets.UTF_8));
        }
        return resp.body();
    }

    public static String uploadMultipart(String url, byte[] data, String filename, String contentType) throws Exception {
        String boundary = "----runify" + System.currentTimeMillis();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"media\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Map<String, Object> json = JacksonUtils.fromJson(resp.body(), new TypeReference<Map<String, Object>>() {
        });
        Integer errcode = asInt(json.get("errcode"));
        if (errcode != null && errcode != 0) {
            throw new RuntimeException("wecom media/upload failed: " + json.get("errmsg"));
        }
        return (String) json.get("media_id");
    }

    public static String md5Hex(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5").digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static Integer asInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
