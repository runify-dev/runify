package com.run.integrations.impl.weixin;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 个人微信 iLink Bot 协议客户端(移植自 hermes weixin.py)。
 * 扫码登录 + 长轮询 getupdates + sendmessage。注意: 非官方接口, 个人号有封号风险。 }
 */
public class IlinkClient {

    public static final String ILINK_BASE_URL = "https://ilinkai.weixin.qq.com";
    public static final String CDN_BASE_URL = "https://novac2c.cdn.weixin.qq.com/c2c";
    private static final String APP_ID = "bot";
    private static final String CHANNEL_VERSION = "2.2.0";
    private static final int CLIENT_VERSION = (2 << 16) | (2 << 8);

    public static final String EP_GET_UPDATES = "ilink/bot/getupdates";
    public static final String EP_SEND_MESSAGE = "ilink/bot/sendmessage";
    public static final String EP_GET_UPLOAD_URL = "ilink/bot/getuploadurl";
    public static final String EP_GET_BOT_QR = "ilink/bot/get_bot_qrcode";
    public static final String EP_GET_QR_STATUS = "ilink/bot/get_qrcode_status";

    public static final int ITEM_TEXT = 1;
    public static final int ITEM_IMAGE = 2;
    public static final int ITEM_VOICE = 3;
    public static final int ITEM_FILE = 4;
    public static final int ITEM_VIDEO = 5;
    public static final int MEDIA_IMAGE = 1;
    public static final int MEDIA_VIDEO = 2;
    public static final int MEDIA_FILE = 3;
    public static final int MSG_TYPE_BOT = 2;
    public static final int MSG_STATE_FINISH = 2;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 获取登录二维码: GET get_bot_qrcode?bot_type=3 -> {qrcode, qrcode_img_content}
     */
    public static JsonObject getBotQrcode() throws Exception {
        return apiGet(ILINK_BASE_URL, EP_GET_BOT_QR + "?bot_type=3", 35_000);
    }

    /**
     * 轮询扫码状态: -> {status: wait/scaned/scaned_but_redirect/expired/confirmed, ...}
     * confirmed 时含 ilink_bot_id/bot_token/baseurl/ilink_user_id
     */
    public static JsonObject getQrcodeStatus(String baseUrl, String qrcode) throws Exception {
        return apiGet(baseUrl, EP_GET_QR_STATUS + "?qrcode=" + enc(qrcode), 35_000);
    }

    /**
     * 长轮询收消息: -> {ret, errcode, get_updates_buf, msgs[], longpolling_timeout_ms}
     */
    public static JsonObject getUpdates(String baseUrl, String token, String syncBuf) throws Exception {
        return apiPost(baseUrl, EP_GET_UPDATES, new JsonObject().put("get_updates_buf", syncBuf == null ? "" : syncBuf), token, 45_000);
    }

    /**
     * 发送文本消息(message_type=BOT, 单条 item_list 文本), 回带 context_token
     */
    public static JsonObject sendText(String baseUrl, String token, String toUserId, String text,
                                      String contextToken, String clientId) throws Exception {
        JsonObject msg = new JsonObject()
                .put("from_user_id", "")
                .put("to_user_id", toUserId)
                .put("client_id", clientId)
                .put("message_type", MSG_TYPE_BOT)
                .put("message_state", MSG_STATE_FINISH)
                .put("item_list", new JsonArray().add(new JsonObject()
                        .put("type", ITEM_TEXT)
                        .put("text_item", new JsonObject().put("text", text))));
        if (contextToken != null && !contextToken.isEmpty()) {
            msg.put("context_token", contextToken);
        }
        return apiPost(baseUrl, EP_SEND_MESSAGE, new JsonObject().put("msg", msg), token, 15_000);
    }

    /**
     * 发送单个媒体 item(图片/视频/文件), item 由调用方构造好
     */
    public static JsonObject sendItem(String baseUrl, String token, String toUserId, JsonObject item,
                                      String contextToken, String clientId) throws Exception {
        JsonObject msg = new JsonObject()
                .put("from_user_id", "")
                .put("to_user_id", toUserId)
                .put("client_id", clientId)
                .put("message_type", MSG_TYPE_BOT)
                .put("message_state", MSG_STATE_FINISH)
                .put("item_list", new JsonArray().add(item));
        if (contextToken != null && !contextToken.isEmpty()) {
            msg.put("context_token", contextToken);
        }
        return apiPost(baseUrl, EP_SEND_MESSAGE, new JsonObject().put("msg", msg), token, 20_000);
    }

    public static JsonObject getUploadUrl(String baseUrl, String token, String toUserId, int mediaType,
                                          String filekey, int rawsize, String rawfilemd5, int filesize,
                                          String aeskeyHex) throws Exception {
        JsonObject payload = new JsonObject()
                .put("filekey", filekey)
                .put("media_type", mediaType)
                .put("to_user_id", toUserId)
                .put("rawsize", rawsize)
                .put("rawfilemd5", rawfilemd5)
                .put("filesize", filesize)
                .put("no_need_thumb", true)
                .put("aeskey", aeskeyHex);
        return apiPost(baseUrl, EP_GET_UPLOAD_URL, payload, token, 15_000);
    }

    /**
     * 上传密文到 CDN, 返回响应头 x-encrypted-param
     */
    public static String uploadCiphertext(String uploadUrl, byte[] ciphertext) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(uploadUrl))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(ciphertext)).build();
        HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("CDN upload HTTP " + resp.statusCode());
        }
        return resp.headers().firstValue("x-encrypted-param")
                .orElseThrow(() -> new RuntimeException("CDN upload missing x-encrypted-param"));
    }

    public static byte[] downloadBytes(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60)).GET().build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
    }

    public static String cdnUploadUrl(String uploadParam, String filekey) {
        return CDN_BASE_URL + "/upload?encrypted_query_param=" + enc(uploadParam) + "&filekey=" + enc(filekey);
    }

    public static String cdnDownloadUrl(String encryptedQueryParam) {
        return CDN_BASE_URL + "/download?encrypted_query_param=" + enc(encryptedQueryParam);
    }

    // ==================== 底层 ====================

    private static JsonObject apiPost(String baseUrl, String endpoint, JsonObject payload, String token, int timeoutMs) throws Exception {
        payload.put("base_info", new JsonObject().put("channel_version", CHANNEL_VERSION));
        String body = payload.encode();
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url(baseUrl, endpoint)))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("AuthorizationType", "ilink_bot_token")
                .header("X-WECHAT-UIN", randomUin())
                .header("iLink-App-Id", APP_ID)
                .header("iLink-App-ClientVersion", String.valueOf(CLIENT_VERSION))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new JsonObject(resp.body());
    }

    private static JsonObject apiGet(String baseUrl, String endpoint, int timeoutMs) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url(baseUrl, endpoint)))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("iLink-App-Id", APP_ID)
                .header("iLink-App-ClientVersion", String.valueOf(CLIENT_VERSION))
                .GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new JsonObject(resp.body());
    }

    private static String url(String baseUrl, String endpoint) {
        return baseUrl.replaceAll("/+$", "") + "/" + endpoint;
    }

    private static String randomUin() {
        byte[] b = new byte[4];
        RANDOM.nextBytes(b);
        long v = ((long) (b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
        return Base64.getEncoder().encodeToString(String.valueOf(v).getBytes(StandardCharsets.UTF_8));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
