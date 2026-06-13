package com.run.route;

import com.run.auth.AggregatePermission;
import com.run.auth.Authenticator;
import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.constants.PermissionConstants;
import com.run.common.route.IRoute;
import com.run.handler.skill.ISkillFileHandler;
import com.run.handler.skill.ISkillFolderHandler;
import com.run.handler.skill.ISkillHandler;
import com.run.handler.skill.impl.SkillFileHandlerImpl;
import com.run.handler.skill.impl.SkillFolderHandlerImpl;
import com.run.handler.skill.impl.SkillHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

public class SkillRoute implements IRoute {
    protected Router apiRoute;
    protected TokenBasicAuthHandler tokenBasicAuthHandler;
    private final ISkillHandler iSkillHandler;
    private final ISkillFolderHandler iSkillFolderHandler;
    private final ISkillFileHandler iSkillFileHandler;
    protected OpenAPI openAPI;

    @Inject
    public SkillRoute(@Named("apiRoute") Router apiRoute, OpenAPI openAPI,
                      @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                      SkillHandlerImpl skillHandler,
                      SkillFolderHandlerImpl skillFolderHandler,
                      SkillFileHandlerImpl skillFileHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.iSkillHandler = skillHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
        this.iSkillFolderHandler = skillFolderHandler;
        this.iSkillFileHandler = skillFileHandler;
    }

    @Override
    public void initRoute() {
        // ===================== 资源 =====================
        apiRoute.get("/skill/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_READ)
                                .addPermission(PermissionConstants.SKILL_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::get);

        apiRoute.put("/skill/resources/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_EDIT)
                                .addPermission(PermissionConstants.SKILL_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::edit);

        apiRoute.delete("/skill/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_DELETE)
                                .addPermission(PermissionConstants.SKILL_DELETE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::delete);

        apiRoute.post("/skill/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_EDIT)
                                .addPermission(PermissionConstants.SKILL_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::rename);

        // ===================== 文件夹 =====================
        apiRoute.post("/skill/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_FOLDER_EDIT)
                                .addPermission(PermissionConstants.SKILL_FOLDER_EDIT.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFolderHandler::rename);

        apiRoute.get("/skill/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_READ)
                                .addPermission(PermissionConstants.SKILL_READ.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFolderHandler::get);

        apiRoute.delete("/skill/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_FOLDER_DELETE)
                                .addPermission(PermissionConstants.SKILL_FOLDER_DELETE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFolderHandler::delete);

        apiRoute.post("/skill/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_FOLDER_CREATE)
                                .addPermission(PermissionConstants.SKILL_FOLDER_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFolderHandler::create);

        apiRoute.get("/skill/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::tree);

        apiRoute.get("/skill/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::list);

        apiRoute.post("/skill/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_CREATE)
                                .addPermission(PermissionConstants.SKILL_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::create);

        // ===================== 导出导入 =====================
        apiRoute.get("/skill/resources/:resourceId/export")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_READ)
                                .addPermission(PermissionConstants.SKILL_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::exportSkill);

        apiRoute.post("/skill/folders/:folderId/resources/import")
                .handler(BodyHandler.create().setUploadsDirectory(System.getProperty("java.io.tmpdir")))
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_CREATE)
                                .addPermission(PermissionConstants.SKILL_CREATE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::importSkill);

        apiRoute.post("/skill/folders/:folderId/resources/install-from-store")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_CREATE)
                                .addPermission(PermissionConstants.SKILL_CREATE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::installFromStore);

        apiRoute.post("/skill/resources/:skillId/upgrade-from-store")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_EDIT)
                                .addPermission(PermissionConstants.SKILL_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillHandler::upgradeFromStore);

        // ===================== 技能文件（详情页文件树） =====================
        apiRoute.get("/skill/resources/:resourceId/files/tree")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFileHandler::tree);

        apiRoute.get("/skill/resources/:resourceId/files/:fileId/children")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFileHandler::listByParent);

        apiRoute.get("/skill/resources/:resourceId/files/:fileId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFileHandler::get);

        apiRoute.post("/skill/resources/:resourceId/files/:fileId/folder")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_EDIT)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFileHandler::createFolder);

        apiRoute.post("/skill/resources/:resourceId/files/:fileId/text")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_EDIT)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFileHandler::createText);

        apiRoute.post("/skill/resources/:resourceId/files/:fileId/upload")
                .handler(BodyHandler.create().setBodyLimit(Long.MAX_VALUE)
                        .setDeleteUploadedFilesOnEnd(true))
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_EDIT)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFileHandler::uploadFile);

        apiRoute.put("/skill/resources/:resourceId/files/:fileId/content")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_EDIT)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFileHandler::updateContent);

        apiRoute.put("/skill/resources/:resourceId/files/:fileId/rename")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_EDIT)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFileHandler::rename);

        apiRoute.delete("/skill/resources/:resourceId/files/:fileId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.SKILL_DELETE)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iSkillFileHandler::delete);

        // ===================== 权限 =====================
        apiRoute.get("/skill/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(iSkillHandler::listResourcePermission);

        apiRoute.put("/skill/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(iSkillHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {
    }
}
