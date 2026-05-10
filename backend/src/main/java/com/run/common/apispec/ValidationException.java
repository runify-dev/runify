package com.run.common.apispec;

import com.run.common.exception.ApiException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends ApiException {

    private final List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super(HttpResponseStatus.BAD_REQUEST.code(), "参数校验失败");
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public JsonObject toResponse() {
        JsonArray arr = new JsonArray();
        for (ValidationError e : errors) {
            arr.add(new JsonObject()
                    .put("source", e.source())
                    .put("field", e.field())
                    .put("message", e.message()));
        }
        return new JsonObject()
                .put("code", code)
                .put("message", getMessage())
                .put("errors", arr);
    }

    public record ValidationError(String source, String field, String message) {
    }
}
