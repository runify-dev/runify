package com.run.integrations.impl.feishu;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 飞书事件订阅解密: key=SHA256(encryptKey), iv=密文前16字节, AES-256-CBC + PKCS7 }
 */
public class FeishuCrypt {

    private final byte[] key;

    public FeishuCrypt(String encryptKey) {
        try {
            this.key = MessageDigest.getInstance("SHA-256").digest(encryptKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encrypt) {
        try {
            byte[] data = Base64.getDecoder().decode(encrypt);
            byte[] iv = Arrays.copyOfRange(data, 0, 16);
            byte[] cipherText = Arrays.copyOfRange(data, 16, data.length);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(cipherText);
            int pad = decrypted[decrypted.length - 1] & 0xff;
            if (pad < 1 || pad > 16) {
                pad = 0;
            }
            return new String(Arrays.copyOfRange(decrypted, 0, decrypted.length - pad), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("feishu decrypt error", e);
        }
    }
}
