package com.run.common.util;

import io.vertx.sqlclient.RowSet;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  22:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class CommonUtils {
    /**
     * 将字符串加密
     *
     * @param input 需要加密字符串
     * @return 加密后的字符串
     */
    public static String getSHA256(String input) {
        try {
            // 获取MessageDigest类的实例，参数为算法名称
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 使用指定的字节更新摘要
            md.update(input.getBytes());
            // 获取密文（即散列值）
            byte[] digest = md.digest();
            // 将得到的字节数组变成字符串返回
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSHA256(File file, Integer capacity) {
        try (FileChannel channel = FileChannel.open(Path.of(file.toURI()))) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(capacity); // 直接内存
            while (channel.read(buffer) != -1) {
                buffer.flip();
                // 处理数据
                byte[] array = buffer.array();
                md.update(array);
                buffer.clear();
            }
            // 获取密文（即散列值）
            byte[] digest = md.digest();
            // 将得到的字节数组变成字符串返回
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSHA256(File file) {
        return getSHA256(file, 64 * 1024);
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 需要转换的字节数组
     * @return 转换后的字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b)); // 将每个字节转换为两位十六进制数，不足两位前面补0
        }
        return sb.toString();
    }

    /**
     * 字符串加密
     *
     * @param message 需要加密的字符串
     * @return 加密后的字符串
     */
    public static String encryption(String message) {
        int max_pre_len = 8;
        int max_post_len = 4;
        int message_len = message.length();
        int pre_len = message_len / 5 * 2;
        int post_len = message_len / 5;
        String pre_str = message.substring(0, pre_len > max_pre_len ? max_pre_len : pre_len <= 0 ? 1 : pre_len);
        String end_str = message.substring(message_len - pre_len < max_post_len ? post_len : max_post_len);
        String content = "***************";
        return pre_str + content + end_str;
    }

    /**
     * 将RowSet<T> 转换为List<T>
     *
     * @param rowSet 需要转换的rowSet
     * @param <T>    列表中的数据类型
     * @return 转换后的List<T>
     */
    public static <T> List<T> toList(RowSet<T> rowSet) {
        List<T> result = new ArrayList<>();
        rowSet.forEach(result::add);
        return result;
    }

}
