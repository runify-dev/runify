package com.run.integrations.impl.weixin;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: iLink 媒体加解密 AES-128-ECB + PKCS7(移植自 hermes weixin.py) }
 */
public class Aes128Ecb {

    public static byte[] encrypt(byte[] plaintext, byte[] key16) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key16, "AES"));
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new RuntimeException("aes128ecb encrypt error", e);
        }
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] key16) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key16, "AES"));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("aes128ecb decrypt error", e);
        }
    }

    /**
     * 灵活解析 aesKey: 可能是 32 位 hex(=16字节) 或 base64(其内容可能又是 hex 字符串)
     */
    public static byte[] parseKey(String aesKey) {
        if (aesKey == null || aesKey.isEmpty()) {
            throw new IllegalArgumentException("empty aesKey");
        }
        if (aesKey.length() == 32 && aesKey.matches("[0-9a-fA-F]+")) {
            return hexToBytes(aesKey);
        }
        byte[] decoded = Base64.getDecoder().decode(aesKey);
        if (decoded.length == 16) {
            return decoded;
        }
        if (decoded.length == 32) {
            String text = new String(decoded, StandardCharsets.US_ASCII);
            if (text.matches("[0-9a-fA-F]+")) {
                return hexToBytes(text);
            }
        }
        throw new IllegalArgumentException("unexpected aesKey format len=" + decoded.length);
    }

    /**
     * 加密后(PKCS7)的长度: ((size+1+15)/16)*16
     */
    public static int paddedSize(int size) {
        return ((size + 1 + 15) / 16) * 16;
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String md5Hex(byte[] data) {
        try {
            return bytesToHex(java.security.MessageDigest.getInstance("MD5").digest(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
