package com.run.auth.dto;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.run.auth.constants.ConversationTypeConstants;
import com.run.common.util.JWTUtil;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/10  18:22}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
@AllArgsConstructor
public class ConversationTokenDTO {
    private static final ConcurrentHashMap<String, ConversationTokenDTO> cache = new ConcurrentHashMap<>();
    /**
     * 类型
     */
    private ConversationTypeConstants type;
    /**
     * 唯一标识
     */
    private String id;
    /**
     * 应用id
     */
    private String applicationId;
    /**
     * 额外参数
     */
    private JsonObject extra;

    public Map<String, Object> toMap() {
        return Map.of("type", type, "id", id, "applicationId", applicationId, "extra", extra.getMap());
    }

    public String toToken() {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.DATE, 1);
        JWTCreator.Builder builder = JWT.create();
        builder.withClaim("type", type.name());
        builder.withClaim("id", id);
        builder.withClaim("applicationId", applicationId);
        Map<String, ?> map = extra.getMap();
        builder.withClaim("extra", map);
        builder.withExpiresAt(instance.getTime());
        return builder.sign(Algorithm.HMAC256(JWTUtil.key));
    }

    public static ConversationTokenDTO newInstance(String jwt) {
        return cache.computeIfAbsent(jwt, key -> {
            Map<String, Claim> stringClaimMap = JWTUtil.decodeToken(key);
            return new ConversationTokenDTO(ConversationTypeConstants.valueOf(stringClaimMap.get("type").asString()), stringClaimMap.get("id").asString(), stringClaimMap.get("applicationId").asString(), new JsonObject(stringClaimMap.get("extra").asMap()));
        });
    }

}
