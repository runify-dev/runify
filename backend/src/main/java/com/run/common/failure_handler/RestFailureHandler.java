package com.run.common.failure_handler;

import com.run.common.exception.ApiException;
import com.run.common.exception.ForbiddenException;
import com.run.common.exception.NotFoundException;
import com.run.common.exception.UnAuthorizedException;
import com.run.common.result.Result;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.validation.BadRequestException;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  16:38}
 * {@code @Version 1.0}
 * {@code @注释: 全局异常处理}
 */
public class RestFailureHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext context) {
        Throwable failure = context.failure();
        if (failure instanceof BadRequestException) {
            context.end(Result.error(failure.getMessage()).toBuffer());
        } else if (failure instanceof HttpException httpException) {
            context.response().setStatusCode(httpException.getStatusCode());
            context.end(Result.error(httpException.getStatusCode(), httpException.getMessage()).toBuffer());
        } else if (failure instanceof UnAuthorizedException unAuthorizedException) {
            context.response().setStatusCode(unAuthorizedException.getCode());
            context.end(Result.error(unAuthorizedException.code, failure.getMessage()).toBuffer());
        } else if (failure instanceof ForbiddenException forbiddenException) {
            context.response().setStatusCode(forbiddenException.getCode());
            context.end(Result.error(forbiddenException.code, failure.getMessage()).toBuffer());
        } else if (failure instanceof NotFoundException notFoundException) {
            context.response().setStatusCode(notFoundException.getCode());
            context.end(Result.error(notFoundException.code, failure.getMessage()).toBuffer());
        } else if (failure instanceof ApiException apiException) {
            context.end(Result.error(apiException.code, failure.getMessage()).toBuffer());
        } else {
            context.end(Result.error(failure.getMessage()).toBuffer());
        }
    }
}
