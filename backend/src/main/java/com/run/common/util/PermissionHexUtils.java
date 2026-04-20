package com.run.common.util;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PermissionHexUtils {

    private PermissionHexUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Map<String, Long> -> Map<String, String>
     * 用于写入 Redis Hash
     */
    public static Map<String, String> toHexMap(Map<String, Long> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>(capacity(map.size()));
        map.forEach((k, v) -> {
            if (k != null && v != null) {
                result.put(k, toHex(v));
            }
        });
        return result;
    }

    /**
     * Map<String, String> -> Map<String, Long>
     * 用于从 Redis Hash 读取后转回 Java 位运算结构
     */
    public static Map<String, Long> fromHexMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Long> result = new HashMap<>(capacity(map.size()));
        map.forEach((k, v) -> {
            if (k != null && v != null && !v.isBlank()) {
                result.put(k, fromHex(v));
            }
        });
        return result;
    }

    /**
     * long -> hex string
     * 例如：511 -> "1ff"
     */
    public static String toHex(long value) {
        return Long.toHexString(value);
    }

    /**
     * hex string -> long
     * 例如："1ff" -> 511
     */
    public static long fromHex(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseUnsignedLong(value, 16);
    }

    /**
     * 判断 mask 是否包含指定 bit
     */
    public static boolean hasBit(long mask, long bit) {
        return (mask & bit) != 0;
    }

    /**
     * 设置 bit
     */
    public static long addBit(long mask, long bit) {
        return mask | bit;
    }

    /**
     * 移除 bit
     */
    public static long removeBit(long mask, long bit) {
        return mask & ~bit;
    }

    /**
     * HashMap 初始化容量，避免扩容
     */
    private static int capacity(int size) {
        return Math.max((int) (size / 0.75f) + 1, 16);
    }
}