package com.run.handler.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: 凭证脱敏: 回显给前端时把敏感字段替换成固定掩码, 用户未修改则原样回传 }
 */
public class IntegrationCredentialMask {

    public static final String MASK = "******";

    /**
     * 需要脱敏的敏感字段(企业微信/飞书/微信)
     */
    private static final Set<String> SECRET_KEYS = Set.of(
            "secret", "appSecret", "encryptKey", "aesKey", "token", "verifyToken"
    );

    public static Map<String, Object> mask(Map<String, Object> config) {
        Map<String, Object> result = new LinkedHashMap<>(config);
        for (String key : SECRET_KEYS) {
            Object value = result.get(key);
            if (value instanceof String s && !s.isEmpty()) {
                result.put(key, MASK);
            }
        }
        return result;
    }
}
