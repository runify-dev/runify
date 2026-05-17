package com.run.route;


import com.run.common.openapi.CommonOpenAPI;
import com.run.common.route.IRoute;
import com.run.handler.file.IFileHandler;
import com.run.handler.file.impl.FileHandlerImpl;
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

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/29  00:38}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class FileRoute implements IRoute {

    protected Router apiRoute;

    protected OpenAPI openAPI;
    protected Router uiRoute;
    protected Router conversationAPIRoute;
    protected IFileHandler iFileHandler;
    protected Router conversationUIRoute;

    @Inject
    public FileRoute(@Named("apiRoute") Router apiRoute,
                     @Named("uiRoute") Router uiRoute,
                     @Named("conversationAPIRoute") Router conversationAPIRoute,
                     @Named("conversationUIRoute") Router conversationUIRoute,
                     OpenAPI openAPI, FileHandlerImpl fileHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.uiRoute = uiRoute;
        this.iFileHandler = fileHandler;
        this.conversationAPIRoute = conversationAPIRoute;
        this.conversationUIRoute = conversationUIRoute;
    }

    @Override
    public void initRoute() {
        apiRoute.post("/storage/file")
                .handler(BodyHandler.create().setBodyLimit(Long.MAX_VALUE)
                        .setDeleteUploadedFilesOnEnd(true))
                .handler(iFileHandler::upload);
        apiRoute.get("/storage/file/:fileId")
                .handler(iFileHandler::download);
        conversationAPIRoute.get("/storage/file/:fileId")
                .handler(iFileHandler::download);

        conversationAPIRoute.post("/storage/file")
                .handler(BodyHandler.create().setBodyLimit(Long.MAX_VALUE)
                        .setDeleteUploadedFilesOnEnd(true))
                .handler(iFileHandler::upload);
        conversationUIRoute.getWithRegex("/.*/api/storage/file/(?<fileId>[^\\/]+)")
                .handler(iFileHandler::download);
        uiRoute.getWithRegex("/.*/api/storage/file/(?<fileId>[^\\/]+)")
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
