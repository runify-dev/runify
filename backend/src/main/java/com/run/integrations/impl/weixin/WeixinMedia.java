package com.run.integrations.impl.weixin;

import io.vertx.core.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: iLink 媒体收发(全程阻塞, 在 poller 虚拟线程里调用):
 * 出站: 下载源 -> AES-128-ECB 加密 -> 上传 CDN -> 构造 image/video/file item;
 * 入站: 从 CDN 下载密文 -> 解密 -> 原始字节。 }
 */
public class WeixinMedia {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 上传一份媒体并构造 item。kind: image/video/file
     */
    public static JsonObject uploadAndBuildItem(String baseUrl, String token, String toUser,
                                                byte[] data, String filename, String kind) throws Exception {
        int mediaType = switch (kind) {
            case "image" -> IlinkClient.MEDIA_IMAGE;
            case "video" -> IlinkClient.MEDIA_VIDEO;
            default -> IlinkClient.MEDIA_FILE;
        };
        byte[] aesKey = new byte[16];
        RANDOM.nextBytes(aesKey);
        String filekey = Aes128Ecb.bytesToHex(randomBytes(16));
        int rawsize = data.length;
        String rawmd5 = Aes128Ecb.md5Hex(data);
        int filesize = Aes128Ecb.paddedSize(rawsize);

        JsonObject up = IlinkClient.getUploadUrl(baseUrl, token, toUser, mediaType, filekey,
                rawsize, rawmd5, filesize, Aes128Ecb.bytesToHex(aesKey));

        byte[] ciphertext = Aes128Ecb.encrypt(data, aesKey);
        String uploadFullUrl = up.getString("upload_full_url", "");
        String uploadParam = up.getString("upload_param", "");
        String uploadUrl = !uploadFullUrl.isEmpty() ? uploadFullUrl
                : IlinkClient.cdnUploadUrl(uploadParam, filekey);
        String encryptedQueryParam = IlinkClient.uploadCiphertext(uploadUrl, ciphertext);

        // iLink 要求 aes_key 为 base64(hex字符串)
        String aesKeyForApi = Base64.getEncoder().encodeToString(
                Aes128Ecb.bytesToHex(aesKey).getBytes(StandardCharsets.US_ASCII));
        JsonObject media = new JsonObject()
                .put("encrypt_query_param", encryptedQueryParam)
                .put("aes_key", aesKeyForApi)
                .put("encrypt_type", 1);

        return switch (kind) {
            case "image" -> new JsonObject().put("type", IlinkClient.ITEM_IMAGE)
                    .put("image_item", new JsonObject().put("media", media).put("mid_size", ciphertext.length));
            case "video" -> new JsonObject().put("type", IlinkClient.ITEM_VIDEO)
                    .put("video_item", new JsonObject().put("media", media)
                            .put("video_size", ciphertext.length).put("play_length", 0).put("video_md5", rawmd5));
            default -> new JsonObject().put("type", IlinkClient.ITEM_FILE)
                    .put("file_item", new JsonObject().put("media", media)
                            .put("file_name", filename).put("len", String.valueOf(rawsize)));
        };
    }

    /**
     * 入站媒体下载并解密。subKey: image_item/video_item/file_item/voice_item
     */
    public static byte[] downloadInbound(JsonObject item, String subKey) throws Exception {
        JsonObject sub = item.getJsonObject(subKey, new JsonObject());
        JsonObject media = sub.getJsonObject("media", new JsonObject());
        String encParam = media.getString("encrypt_query_param");
        String fullUrl = media.getString("full_url");
        // image 的 aeskey 常在 image_item.aeskey(hex); 其它在 media.aes_key
        String aesKey = sub.getString("aeskey", media.getString("aes_key"));

        byte[] raw;
        if (encParam != null && !encParam.isEmpty()) {
            raw = IlinkClient.downloadBytes(IlinkClient.cdnDownloadUrl(encParam));
        } else if (fullUrl != null && !fullUrl.isEmpty()) {
            raw = IlinkClient.downloadBytes(fullUrl);
        } else {
            throw new RuntimeException("media item has neither encrypt_query_param nor full_url");
        }
        if (aesKey != null && !aesKey.isEmpty()) {
            raw = Aes128Ecb.decrypt(raw, Aes128Ecb.parseKey(aesKey));
        }
        return raw;
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }
}
