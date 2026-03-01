package com.run.common.safeapi;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/2/27  18:52}
 * {@code @Version 1.0}
 * {@code @注释: 一些工具}
 */
public class CommonAPI {
    @HostAccess.Export
    public ProxyExecutable md5 = arguments -> {
        String input = arguments[0].asString();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    };
    @HostAccess.Export
    public ProxyExecutable uuid = arguments -> {
        return UUID.randomUUID().toString();
    };
}
