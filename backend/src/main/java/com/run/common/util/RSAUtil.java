package com.run.common.util;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/3  22:35}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class RSAUtil {
    // 加密块大小 (2048位密钥对应256字节，减去PKCS#1填充的11字节)
    private static final int ENCRYPT_BLOCK_SIZE = 245;

    // 解密块大小 (2048位密钥对应256字节)
    private static final int DECRYPT_BLOCK_SIZE = 256;

    private static KeyPair keyPair;

    public static void setKeyPair(KeyPair keyPair) {
        RSAUtil.keyPair = keyPair;
    }

    // 获取Base64编码的公钥字符串
    public static String getPublicKey(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    // 获取Base64编码的私钥字符串
    public static String getPrivateKey(KeyPair keyPair) {
        PrivateKey privateKey = keyPair.getPrivate();
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    // 从Base64字符串加载公钥
    @SneakyThrows
    public static PublicKey loadPublicKey(String base64PublicKey)  {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    // 从Base64字符串加载私钥
    @SneakyThrows
    public static PrivateKey loadPrivateKey(String base64PrivateKey)  {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    // 生成 RSA 密钥对
    public static KeyPair generateKeyPair() {
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyPairGenerator.initialize(2048); // 密钥长度
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 加密
     *
     * @param data 需要加密的数据
     * @return 加密后的数据
     */
    public static String encrypt(String data) {
        try {
            return encrypt(data, keyPair.getPublic());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 解密
     *
     * @param data 需要解密的数据
     * @return 解密后的数据
     */
    public static String decrypt(String data) {
        try {
            return decrypt(data, keyPair.getPrivate());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // 分段加密
    public static String encrypt(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        int inputLen = dataBytes.length;
        int offSet = 0;
        byte[] buffer;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > ENCRYPT_BLOCK_SIZE) {
                buffer = cipher.doFinal(dataBytes, offSet, ENCRYPT_BLOCK_SIZE);
            } else {
                buffer = cipher.doFinal(dataBytes, offSet, inputLen - offSet);
            }
            out.write(buffer);
            offSet += ENCRYPT_BLOCK_SIZE;
        }

        byte[] encryptedData = out.toByteArray();
        out.close();

        return Base64.getEncoder().encodeToString(encryptedData);
    }

    // 分段解密
    public static String decrypt(String encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] dataBytes = Base64.getDecoder().decode(encryptedData);
        int inputLen = dataBytes.length;
        int offSet = 0;
        byte[] buffer;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > DECRYPT_BLOCK_SIZE) {
                buffer = cipher.doFinal(dataBytes, offSet, DECRYPT_BLOCK_SIZE);
            } else {
                buffer = cipher.doFinal(dataBytes, offSet, inputLen - offSet);
            }
            out.write(buffer);
            offSet += DECRYPT_BLOCK_SIZE;
        }

        byte[] decryptedData = out.toByteArray();
        out.close();
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}
