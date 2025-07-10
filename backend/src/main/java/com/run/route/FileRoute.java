package com.run.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.run.common.openapi.CommonOpenAPI;
import com.run.common.route.IRoute;
import com.run.handler.file.IFileHandler;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/29  00:38}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class FileRoute implements IRoute {
    @Inject
    @Named("apiRoute")
    protected Router apiRoute;
    @Inject
    protected OpenAPI openAPI;
    @Inject
    protected IFileHandler iFileHandler;

    @Override
    public void initRoute() {
        apiRoute.post("/file")
                .handler(BodyHandler.create().setBodyLimit(Long.MAX_VALUE)
                        .setDeleteUploadedFilesOnEnd(true))
                .handler(iFileHandler::upload);
        apiRoute.get("/file/:file_id")
                .handler(iFileHandler::download);
    }

    @Override
    public void initOpenApi() {
        openAPI.path("/api/file", new PathItem()
                .post(new Operation()
                        .tags(List.of("文件"))
                        .security(CommonOpenAPI.getSecurity())
                        .description("上传文件")
                        .requestBody(new RequestBody()
                                .content(new Content()
                                        .addMediaType("multipart/form-data",
                                                new MediaType()
                                                        .schema(new ObjectSchema()
                                                                .required(List.of("file"))
                                                                .addProperty("file", new FileSchema()
                                                                        .description("文件").name("file"))

                                                        ))

                                ))));

        openAPI.path("/api/file/{file_id}",
                new PathItem().get(new Operation().tags(List.of("文件"))
                        .description("下载文件")
                        .parameters(List.of(
                                new Parameter().name("file_id")
                                        .in("path").required(true)
                                        .description("文件id")))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse()
                                        .content(new Content()
                                                .addMediaType("application/octet-stream",
                                                        new MediaType()
                                                                .schema(new BinarySchema()
                                                                )))))));
    }


}
