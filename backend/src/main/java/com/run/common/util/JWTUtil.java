package com.run.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.run.common.exception.UnAuthorizedException;

import java.util.Calendar;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/4  23:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class JWTUtil {

    /**
     * 密钥不要随意改动
     */
    private static final String key = "123123123";

    /**
     * 生成token
     *
     * @param map 放在payload中的信息
     * @return
     */
    public static String getToken(Map<String, String> map) {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.DATE, 1);
        JWTCreator.Builder builder = JWT.create();
        map.forEach(builder::withClaim);
        builder.withExpiresAt(instance.getTime());
        return builder.sign(Algorithm.HMAC256(key));

    }

    /**
     * 验证token
     *
     * @param token
     * @return
     */
    public static DecodedJWT verify(String token) {
        return JWT.require(Algorithm.HMAC256(key)).build().verify(token);
    }

    /**
     * 解析token
     *
     * @param token 需要解析的token
     * @return 解析后的数据
     */
    public static Map<String, Claim> decodeToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(key)).build().verify(token);
            if (jwt != null) {
                return jwt.getClaims();
            }
            throw new UnAuthorizedException("token无效！");
        } catch (SignatureVerificationException e) {
            throw new UnAuthorizedException("无效签名！");
        } catch (TokenExpiredException e) {
            throw new UnAuthorizedException("token过期");
        } catch (AlgorithmMismatchException e) {
            throw new UnAuthorizedException("算法不一致");
        } catch (Exception e) {
            throw new UnAuthorizedException("token无效！");
        }
    }
}
