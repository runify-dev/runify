package com.run.integrations.impl.wecomstream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/28  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信智能机器人长连接 入站媒体下载/解密:
 * 媒体帧给 {url(加密下载链接,5分钟有效), aeskey(每链接唯一,Base64)}。
 * 下载密文 -> AES-256-CBC 解密(key=Base64解码后32字节, IV=key前16字节, 关闭自动padding后手动去PKCS#7,
 * 填充按 1..32 字节)。文件名取自响应 Content-Disposition(帧内不带文件名)。 }
 */
public final class WecomStreamMedia {

    private WecomStreamMedia() {
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final Pattern CD_UTF8 = Pattern.compile("filename\\*=UTF-8''([^;\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CD_PLAIN = Pattern.compile("filename=\"?([^\";\\s]+)\"?", Pattern.CASE_INSENSITIVE);

    public record Media(byte[] data, String filename) {
    }

    /**
     * 下载并(按需)解密一份媒体。aesKey 为空则返回原始字节。
     */
    public static Media fetch(String url, String aesKey) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60)).GET().build();
        HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("media download HTTP " + resp.statusCode());
        }
        byte[] data = resp.body();
        if (aesKey != null && !aesKey.isEmpty()) {
            data = decrypt(data, aesKey);
        }
        String name = parseFilename(resp.headers().firstValue("content-disposition").orElse(null));
        return new Media(data, name);
    }

    /**
     * AES-256-CBC 解密: key=Base64(aesKey), IV=key 前 16 字节, 手动去 PKCS#7(填充 1..32)
     */
    public static byte[] decrypt(byte[] cipherBytes, String aesKeyB64) throws Exception {
        byte[] key = Base64.getDecoder().decode(aesKeyB64);
        byte[] iv = Arrays.copyOfRange(key, 0, 16);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] out = cipher.doFinal(cipherBytes);
        int pad = out[out.length - 1] & 0xff;
        if (pad < 1 || pad > 32 || pad > out.length) {
            throw new RuntimeException("invalid PKCS#7 padding: " + pad);
        }
        for (int i = out.length - pad; i < out.length; i++) {
            if ((out[i] & 0xff) != pad) {
                throw new RuntimeException("invalid PKCS#7 padding bytes");
            }
        }
        return Arrays.copyOfRange(out, 0, out.length - pad);
    }

    /**
     * 从 Content-Disposition 解析文件名(优先 RFC 5987 的 filename*=UTF-8''xxx)
     */
    public static String parseFilename(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isEmpty()) {
            return null;
        }
        Matcher u = CD_UTF8.matcher(contentDisposition);
        if (u.find()) {
            return URLDecoder.decode(u.group(1), StandardCharsets.UTF_8);
        }
        Matcher p = CD_PLAIN.matcher(contentDisposition);
        if (p.find()) {
            return URLDecoder.decode(p.group(1), StandardCharsets.UTF_8);
        }
        return null;
    }
}
