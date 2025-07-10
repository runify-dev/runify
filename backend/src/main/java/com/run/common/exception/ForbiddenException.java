package com.run.common.exception;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:33}
 * {@code @Version 1.0}
 * {@code @注释: 没有权限异常}
 */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(403, message);
    }

    public static ForbiddenException of(String message) {
        return new ForbiddenException(message);
    }
}
