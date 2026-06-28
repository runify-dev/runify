package com.run.route;

import com.run.auth.AggregatePermission;
import com.run.auth.Authenticator;
import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.constants.PermissionConstants;
import com.run.common.route.IRoute;
import com.run.handler.integration.IIntegrationFolderHandler;
import com.run.handler.integration.IIntegrationHandler;
import com.run.handler.integration.impl.IntegrationFolderHandlerImpl;
import com.run.handler.integration.impl.IntegrationHandlerImpl;
import com.run.handler.integration.impl.WeixinAuthHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: 第三方集成后台管理路由 }
 */
public class IntegrationRoute implements IRoute {
    protected Router apiRoute;

    protected TokenBasicAuthHandler tokenBasicAuthHandler;

    private final IIntegrationHandler integrationHandler;
    private final IIntegrationFolderHandler integrationFolderHandler;
    private final WeixinAuthHandler weixinAuthHandler;

    @Inject
    public IntegrationRoute(@Named("apiRoute") Router apiRoute,
                            @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                            IntegrationHandlerImpl integrationHandler,
                            IntegrationFolderHandlerImpl integrationFolderHandler,
                            WeixinAuthHandler weixinAuthHandler) {
        this.apiRoute = apiRoute;
        this.integrationHandler = integrationHandler;
        this.integrationFolderHandler = integrationFolderHandler;
        this.weixinAuthHandler = weixinAuthHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
    }

    @Override
    public void initRoute() {
        // 平台类型目录(静态元数据): 供前端新建集成渲染类型下拉与凭证表单
        apiRoute.get("/integration/types")
                .handler(tokenBasicAuthHandler)
                .handler(integrationHandler::getTypes);

        apiRoute.get("/integration/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_READ)
                                .addPermission(PermissionConstants.INTEGRATION_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationHandler::get);

        // 微信(个人号/iLink) 扫码登录
        apiRoute.get("/integration/resources/:resourceId/weixin/qrcode")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(weixinAuthHandler::qrcode);

        apiRoute.get("/integration/resources/:resourceId/weixin/qrcode-status")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(weixinAuthHandler::qrcodeStatus);

        apiRoute.put("/integration/resources/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_EDIT)
                                .addPermission(PermissionConstants.INTEGRATION_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationHandler::edit);

        apiRoute.delete("/integration/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_DELETE)
                                .addPermission(PermissionConstants.INTEGRATION_DELETE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationHandler::delete);

        apiRoute.post("/integration/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_EDIT)
                                .addPermission(PermissionConstants.INTEGRATION_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationHandler::rename);

        apiRoute.post("/integration/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_EDIT)
                                .addPermission(PermissionConstants.INTEGRATION_EDIT.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationFolderHandler::rename);

        apiRoute.get("/integration/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_READ)
                                .addPermission(PermissionConstants.INTEGRATION_READ.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationFolderHandler::get);

        apiRoute.delete("/integration/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_DELETE)
                                .addPermission(PermissionConstants.INTEGRATION_DELETE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationFolderHandler::delete);

        apiRoute.post("/integration/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_FOLDER_CREATE)
                                .addPermission(PermissionConstants.INTEGRATION_FOLDER_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationFolderHandler::create);

        apiRoute.get("/integration/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationHandler::tree);

        apiRoute.get("/integration/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationHandler::list);

        apiRoute.post("/integration/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.INTEGRATION_CREATE)
                                .addPermission(PermissionConstants.INTEGRATION_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(integrationHandler::create);

        apiRoute.get("/integration/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(integrationHandler::listResourcePermission);

        apiRoute.put("/integration/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(integrationHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {

    }
}
