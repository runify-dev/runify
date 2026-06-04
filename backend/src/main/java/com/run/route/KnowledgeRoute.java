package com.run.route;

import com.run.auth.AggregatePermission;
import com.run.auth.Authenticator;
import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.constants.PermissionConstants;
import com.run.common.route.IRoute;
import com.run.handler.knowledge.IDocumentHandler;
import com.run.handler.knowledge.IKnowledgeFolderHandler;
import com.run.handler.knowledge.IKnowledgeHandler;
import com.run.handler.knowledge.impl.DocumentHandlerImpl;
import com.run.handler.knowledge.impl.KnowledgeFolderHandlerImpl;
import com.run.handler.knowledge.impl.KnowledgeHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

public class KnowledgeRoute implements IRoute {
    protected Router apiRoute;
    protected TokenBasicAuthHandler tokenBasicAuthHandler;
    private final IKnowledgeHandler iKnowledgeHandler;
    private final IKnowledgeFolderHandler iKnowledgeFolderHandler;
    private final IDocumentHandler iDocumentHandler;
    protected OpenAPI openAPI;

    @Inject
    public KnowledgeRoute(@Named("apiRoute") Router apiRoute, OpenAPI openAPI,
                          @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                          KnowledgeHandlerImpl knowledgeHandler,
                          KnowledgeFolderHandlerImpl knowledgeFolderHandler,
                          DocumentHandlerImpl documentHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.iKnowledgeHandler = knowledgeHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
        this.iKnowledgeFolderHandler = knowledgeFolderHandler;
        this.iDocumentHandler = documentHandler;
    }

    @Override
    public void initRoute() {
        // ===================== 资源 =====================
        apiRoute.get("/knowledge/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_READ)
                                .addPermission(PermissionConstants.KNOWLEDGE_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeHandler::get);

        apiRoute.put("/knowledge/resources/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_EDIT)
                                .addPermission(PermissionConstants.KNOWLEDGE_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeHandler::edit);

        apiRoute.delete("/knowledge/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_DELETE)
                                .addPermission(PermissionConstants.KNOWLEDGE_DELETE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeHandler::delete);

        apiRoute.post("/knowledge/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_EDIT)
                                .addPermission(PermissionConstants.KNOWLEDGE_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeHandler::rename);

        // ===================== 文件夹 =====================
        apiRoute.post("/knowledge/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_FOLDER_EDIT)
                                .addPermission(PermissionConstants.KNOWLEDGE_FOLDER_EDIT.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeFolderHandler::rename);

        apiRoute.get("/knowledge/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_READ)
                                .addPermission(PermissionConstants.KNOWLEDGE_READ.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeFolderHandler::get);

        apiRoute.delete("/knowledge/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_FOLDER_DELETE)
                                .addPermission(PermissionConstants.KNOWLEDGE_FOLDER_DELETE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeFolderHandler::delete);

        apiRoute.post("/knowledge/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_FOLDER_CREATE)
                                .addPermission(PermissionConstants.KNOWLEDGE_FOLDER_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeFolderHandler::create);

        apiRoute.get("/knowledge/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeHandler::tree);

        apiRoute.get("/knowledge/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeHandler::list);

        apiRoute.post("/knowledge/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_CREATE)
                                .addPermission(PermissionConstants.KNOWLEDGE_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iKnowledgeHandler::create);

        // ===================== 文档 =====================
        apiRoute.get("/knowledge/resources/:resourceId/documents/tree")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iDocumentHandler::tree);

        apiRoute.get("/knowledge/resources/:resourceId/documents/:documentId/children")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iDocumentHandler::listByParent);

        apiRoute.get("/knowledge/resources/:resourceId/documents/:documentId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iDocumentHandler::get);

        apiRoute.post("/knowledge/resources/:resourceId/documents/:documentId/folder")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_EDIT)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iDocumentHandler::createFolder);

        apiRoute.post("/knowledge/resources/:resourceId/documents/:documentId/text")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_EDIT)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iDocumentHandler::createText);

        apiRoute.put("/knowledge/resources/:resourceId/documents/:documentId/content")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_EDIT)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iDocumentHandler::updateContent);

        apiRoute.put("/knowledge/resources/:resourceId/documents/:documentId/rename")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_EDIT)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iDocumentHandler::rename);

        apiRoute.delete("/knowledge/resources/:resourceId/documents/:documentId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.KNOWLEDGE_DELETE)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iDocumentHandler::delete);

        // ===================== 权限 =====================
        apiRoute.get("/knowledge/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(iKnowledgeHandler::listResourcePermission);

        apiRoute.put("/knowledge/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(iKnowledgeHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {
    }
}
