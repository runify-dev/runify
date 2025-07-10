package com.run.common.exception;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:31}
 * {@code @Version 1.0}
 * {@code @注释: 未认证(未登录)异常}
 */
public class UnAuthorizedException extends ApiException {

    public UnAuthorizedException(String message) {
        super(401, message);
    }

    public static UnAuthorizedException of(String message) {
        return new UnAuthorizedException(message);
    }
}
