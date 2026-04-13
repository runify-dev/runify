package com.run.route;


import com.run.auth.TokenBasicAuthHandler;
import com.run.common.openapi.CommonOpenAPI;
import com.run.common.route.IRoute;
import com.run.handler.note.INoteFolderHandler;
import com.run.handler.note.INoteHandler;
import com.run.handler.note.impl.NoteFolderHandlerImpl;
import com.run.handler.note.impl.NoteHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/14  22:42}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NoteRoute implements IRoute {
    protected Router apiRoute;

    protected TokenBasicAuthHandler tokenBasicAuthHandler;

    private final INoteHandler iNoteHandler;

    private final INoteFolderHandler iNoteFolderHandler;

    protected OpenAPI openAPI;

    @Inject
    public NoteRoute(@Named("apiRoute") Router apiRoute, OpenAPI openAPI,
                     @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                     NoteHandlerImpl noteHandler,
                     NoteFolderHandlerImpl noteFolderHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.iNoteHandler = noteHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
        this.iNoteFolderHandler = noteFolderHandler;
    }

    @Override
    public void initRoute() {
        apiRoute.get("/note/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(iNoteHandler::get);

        apiRoute.put("/note/resources/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNoteHandler::edit);

        apiRoute.delete("/note/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(iNoteHandler::delete);

        apiRoute.post("/note/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNoteHandler::rename);

        apiRoute.post("/note/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNoteFolderHandler::rename);

        apiRoute.get("/note/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(iNoteFolderHandler::get);

        apiRoute.delete("/note/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(iNoteFolderHandler::delete);

        apiRoute.post("/note/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNoteFolderHandler::create);

        apiRoute.get("/note/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(iNoteHandler::tree);

        apiRoute.get("/note/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(iNoteHandler::list);

        apiRoute.post("/note/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNoteHandler::create);

        apiRoute.get("/note/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(iNoteHandler::listResourcePermission);

        apiRoute.put("/note/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(iNoteHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {
        openAPI.path("/api/note/markdown/{node_id}", new PathItem()
//                .get(new Operation()
//                        .security(CommonOpenAPI.getSecurity())
//                        .tags(List.of("知识库")).description("获取markdown知识库详情")
//                        .parameters(List.of(new Parameter().name("node_id").description("节点id").in("path").required(true)))
//                        .responses(CommonOpenAPI.getApiResponse(MarkdownNode::getSchema)))
                .put(new Operation().description("修改markdown知识库")
                        .tags(List.of("知识库"))
                        .security(CommonOpenAPI.getSecurity())
                        .parameters(List.of(new Parameter().name("node_id").description("节点id").in("path").required(true)))
                        .requestBody(new RequestBody().content(new Content().addMediaType("application/json", new MediaType().schema(
                                new JsonSchema().required(List.of("content")).addProperty("content", new StringSchema())
                        ))))));

    }
}
