package com.run.common.result;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/14  00:05}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class Result<T> {
    private Integer code;

    private String message;

    private T data;


    public Result<T> message(String message) {
        this.message = message;
        return this;
    }

    public Result<T> code(Integer code) {
        this.code = code;
        return this;
    }

    public Result<T> data(T data) {
        this.data = data;
        return this;
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .code(HttpResponseStatus.OK.code())
                .data(data);
    }

    public static <T> Result<T> error(T data) {
        return new Result<T>()
                .code(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                .data(data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<T>()
                .code(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                .message(message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<T>()
                .code(code)
                .message(message);
    }

    public Buffer toBuffer() {
        return Json.encodeToBuffer(this);
    }

}