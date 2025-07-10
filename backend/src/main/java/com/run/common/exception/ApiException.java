package com.run.common.exception;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:29}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

public class ApiException extends RuntimeException {
    public int code;
    private String message;

    public ApiException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiException of(int code, String message) {
        return new ApiException(code, message);
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
