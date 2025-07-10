package com.run.common.openapi;

import com.google.common.base.Supplier;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/5/5  23:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class CommonOpenAPI {
    public static ApiResponses getApiResponse(Schema<?> dataSchema) {
        return getApiResponse(() -> dataSchema);
    }

    /**
     * @param dataSchema 数据Schema
     * @return openapi响应
     */
    public static ApiResponses getApiResponse(Supplier<Schema<?>> dataSchema) {
        return new ApiResponses().addApiResponse("200", new ApiResponse()
                .description("成功响应")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new JsonSchema().addProperty("code", new IntegerSchema())
                                        .addProperty("data", dataSchema.get())
                                        .addProperty("message", new StringSchema())
                                )
                        )));

    }

    public static ApiResponses getPageApiResponse(Schema<?> recordsSchema) {
        return getPageApiResponse(() -> recordsSchema);
    }

    /**
     * 分页响应
     *
     * @param recordsSchema 数据Schema
     * @return openapi响应
     */
    public static ApiResponses getPageApiResponse(Supplier<Schema<?>> recordsSchema) {
        return new ApiResponses().addApiResponse("200", new ApiResponse()
                .description("成功响应")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new JsonSchema().addProperty("code", new IntegerSchema())
                                        .addProperty("data", new JsonSchema()
                                                .addProperty("total", new IntegerSchema().description("总条数"))
                                                .addProperty("records", new ArraySchema().items(recordsSchema.get()))
                                                .addProperty("current", new IntegerSchema().description("当前页"))
                                                .addProperty("size", new IntegerSchema().description("每页大小")))
                                        .addProperty("message", new StringSchema())
                                )
                        )));
    }

    public static ApiResponses getArrayApiResponse(Schema<?> dataSchema) {
        return getArrayApiResponse(() -> dataSchema);
    }

    /**
     * 列表响应
     *
     * @param dataSchema 数据Schema
     * @return openapi响应
     */
    public static ApiResponses getArrayApiResponse(Supplier<Schema<?>> dataSchema) {
        return new ApiResponses().addApiResponse("200", new ApiResponse()
                .description("成功响应")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new JsonSchema().addProperty("code", new IntegerSchema())
                                        .addProperty("data", new ArraySchema().items(dataSchema.get()))
                                        .addProperty("message", new StringSchema())
                                )
                        )));

    }

    public static RequestBody getApiRequest(Schema<?> dataSchema) {
        return getApiRequest(() -> dataSchema);
    }

    public static RequestBody getApiRequest(Supplier<Schema<?>> dataSchema) {
        return new RequestBody().content(new Content().addMediaType("application/json", new MediaType()
                .schema(dataSchema.get())));
    }

    public static List<SecurityRequirement> getSecurity() {
        return List.of(new SecurityRequirement().addList("tokenAuth"));
    }
}
