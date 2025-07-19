package com.run.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.run.auth.TokenBasicAuthHandler;
import com.run.common.openapi.CommonOpenAPI;
import com.run.common.route.IRoute;
import com.run.handler.knowledge.IKnowledgeHandler;
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

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/14  22:42}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class KnowledgeRoute implements IRoute {
    @Inject
    @Named("apiRoute")
    protected Router apiRoute;
    @Inject
    protected TokenBasicAuthHandler tokenBasicAuthHandler;
    @Inject
    private IKnowledgeHandler iKnowledgeHandler;
    @Inject
    protected OpenAPI openAPI;


    @Override
    public void initRoute() {
        apiRoute.put("/knowledge/folder/:folderId/resource/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iKnowledgeHandler::edit);
    }

    @Override
    public void initOpenApi() {
        openAPI.path("/api/knowledge/markdown/{node_id}", new PathItem()
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
