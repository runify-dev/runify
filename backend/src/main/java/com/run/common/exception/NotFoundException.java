package com.run.common.exception;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:35}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(404, message);
    }

    public static NotFoundException of(String message) {
        return new NotFoundException(message);
    }
}