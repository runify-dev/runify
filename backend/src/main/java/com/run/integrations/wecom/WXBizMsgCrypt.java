package com.run.integrations.wecom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信回调消息加解密(标准算法: AES-256-CBC + PKCS7 + SHA1 签名), 应用/机器人共用 }
 */
public class WXBizMsgCrypt {

    private final byte[] aesKey;
    private final String token;
    private final String receiveId;
    /**
     * 最近一次 decrypt 得到的发送方 receiveId(回复加密时需要原样带回)
     */
    private String lastReceiveId;

    public WXBizMsgCrypt(String token, String encodingAesKey, String receiveId) {
        this.token = token;
        this.receiveId = receiveId;
        this.aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
    }

    public String getLastReceiveId() {
        return lastReceiveId;
    }

    public static String sha1(String token, String timestamp, String nonce, String encrypt) {
        try {
            String[] arr = new String[]{token, timestamp, nonce, encrypt};
            Arrays.sort(arr);
            StringBuilder sb = new StringBuilder();
            for (String s : arr) {
                sb.append(s);
            }
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("sha1 error", e);
        }
    }

    public boolean verifySignature(String signature, String timestamp, String nonce, String encrypt) {
        return signature != null && signature.equals(sha1(token, timestamp, nonce, encrypt));
    }

    public String getToken() {
        return token;
    }

    public String decrypt(String encrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypt));
            byte[] bytes = pkcs7Decode(original);
            int msgLen = bytesToInt(Arrays.copyOfRange(bytes, 16, 20));
            String content = new String(Arrays.copyOfRange(bytes, 20, 20 + msgLen), StandardCharsets.UTF_8);
            String fromReceiveId = new String(Arrays.copyOfRange(bytes, 20 + msgLen, bytes.length), StandardCharsets.UTF_8);
            this.lastReceiveId = fromReceiveId;
            if (receiveId != null && !receiveId.isEmpty() && !receiveId.equals(fromReceiveId)) {
                throw new RuntimeException("receiveId mismatch");
            }
            return content;
        } catch (Exception e) {
            throw new RuntimeException("decrypt error", e);
        }
    }

    public String encrypt(String plainText) {
        try {
            byte[] random = randomBytes();
            byte[] textBytes = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] networkLen = intToBytes(textBytes.length);
            byte[] receiveIdBytes = receiveId.getBytes(StandardCharsets.UTF_8);

            byte[] unpadded = new byte[random.length + networkLen.length + textBytes.length + receiveIdBytes.length];
            int pos = 0;
            System.arraycopy(random, 0, unpadded, pos, random.length);
            pos += random.length;
            System.arraycopy(networkLen, 0, unpadded, pos, networkLen.length);
            pos += networkLen.length;
            System.arraycopy(textBytes, 0, unpadded, pos, textBytes.length);
            pos += textBytes.length;
            System.arraycopy(receiveIdBytes, 0, unpadded, pos, receiveIdBytes.length);

            byte[] padded = pkcs7Encode(unpadded);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            return Base64.getEncoder().encodeToString(cipher.doFinal(padded));
        } catch (Exception e) {
            throw new RuntimeException("encrypt error", e);
        }
    }

    private static int bytesToInt(byte[] b) {
        return ((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
    }

    private static byte[] intToBytes(int n) {
        return new byte[]{(byte) ((n >> 24) & 0xff), (byte) ((n >> 16) & 0xff), (byte) ((n >> 8) & 0xff), (byte) (n & 0xff)};
    }

    private static byte[] randomBytes() {
        byte[] b = new byte[16];
        new java.security.SecureRandom().nextBytes(b);
        return b;
    }

    private static final int BLOCK_SIZE = 32;

    private static byte[] pkcs7Encode(byte[] source) {
        int amountToPad = BLOCK_SIZE - (source.length % BLOCK_SIZE);
        if (amountToPad == 0) {
            amountToPad = BLOCK_SIZE;
        }
        byte pad = (byte) (amountToPad & 0xff);
        byte[] result = Arrays.copyOf(source, source.length + amountToPad);
        for (int i = source.length; i < result.length; i++) {
            result[i] = pad;
        }
        return result;
    }

    private static byte[] pkcs7Decode(byte[] decrypted) {
        int pad = decrypted[decrypted.length - 1] & 0xff;
        if (pad < 1 || pad > BLOCK_SIZE) {
            pad = 0;
        }
        return Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);
    }
}
