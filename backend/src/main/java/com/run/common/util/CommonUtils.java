package com.run.common.util;

import com.github.f4b6a3.uuid.UuidCreator;
import io.vertx.sqlclient.RowSet;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  22:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class CommonUtils {
    public static String getFileContent(String path) {
        try (InputStream is = CommonUtils.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

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

    @SneakyThrows
    public static void copyProperties(Object source, Object target) {
        BeanUtils.copyProperties(target, source);
    }

    public static String getSHA256(File file, Integer capacity) {
        try (FileChannel channel = FileChannel.open(Path.of(file.toURI()))) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(capacity); // 直接内存
            while (channel.read(buffer) != -1) {
                buffer.flip();
                // 处理数据
                byte[] array = buffer.array();
                md.update(array, 0, buffer.limit());
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
        if (StringUtils.isEmpty(message)) {
            return "***************";
        }
        int maxPreLen = 8;
        int maxPostLen = 4;
        int messageLen = message.length();

        int preLen = (int) (messageLen / 5.0 * 2);
        int postLen = (int) (messageLen / 5.0 * 1);

        // 计算前缀长度
        int actualPreLen = preLen;
        if (preLen > maxPreLen) {
            actualPreLen = maxPreLen;
        } else if (preLen <= 0) {
            actualPreLen = 1;
        }

        // 计算后缀长度
        int actualPostLen = (preLen < maxPostLen) ? postLen : maxPostLen;
        if (actualPostLen < 0) {
            actualPostLen = 0;
        }

        // 获取前缀字符串
        String preStr = message.substring(0, Math.min(actualPreLen, messageLen));

        // 获取后缀字符串
        int postStart = messageLen - Math.min(actualPostLen, messageLen);
        String endStr = message.substring(postStart);

        String content = "***************";
        return preStr + content + endStr;
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

    public static Path getOssPath() {
        return Paths.get("data/oss" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("/yyyy/MM/dd/HH/")) + UUID.randomUUID()).toAbsolutePath();
    }

    public static UUID uuid7() {
        return UuidCreator.getTimeOrderedEpoch();

    }

    public static <K, V> Map<K, V> ofNullable(Object... kv) {
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((K) kv[i], (V) kv[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}