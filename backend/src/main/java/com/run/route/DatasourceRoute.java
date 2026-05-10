package com.run.route;

import com.run.auth.AggregatePermission;
import com.run.auth.Authenticator;
import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.constants.PermissionConstants;
import com.run.common.route.IRoute;
import com.run.handler.datasource.IDataSourceFolderHandler;
import com.run.handler.datasource.IDataSourceHandler;
import com.run.handler.datasource.impl.DataSourceFolderHandlerImpl;
import com.run.handler.datasource.impl.DataSourceHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/1  22:41}
 * {@code @Version 1.0}
 * {@code @注释: 数据源连接池路由}
 */
public class DatasourceRoute implements IRoute {
    protected Router apiRoute;

    protected TokenBasicAuthHandler tokenBasicAuthHandler;

    private final IDataSourceHandler dataSourceHandler;
    private final IDataSourceFolderHandler dataSourceFolderHandler;

    protected OpenAPI openAPI;

    @Inject
    public DatasourceRoute(@Named("apiRoute") Router apiRoute,
                         OpenAPI openAPI,
                         @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                         DataSourceHandlerImpl dataSourceHandler,
                         DataSourceFolderHandlerImpl dataSourceFolderHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.dataSourceHandler = dataSourceHandler;
        this.dataSourceFolderHandler = dataSourceFolderHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
    }

    @Override
    public void initRoute() {
        // 数据源类型和供应商接口
        apiRoute.get("/database-collection-pool/types")
                .handler(tokenBasicAuthHandler)
                .handler(dataSourceHandler::getDataSourceTypes);
        apiRoute.get("/database-collection-pool/types/:type/providers")
                .handler(tokenBasicAuthHandler)
                .handler(dataSourceHandler::getProviders);
        apiRoute.get("/database-collection-pool/providers/:provider/form")
                .handler(tokenBasicAuthHandler)
                .handler(dataSourceHandler::getFormDefinition);

        // 资源 CRUD
        apiRoute.get("/datasource/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_READ)
                                .addPermission(PermissionConstants.DATASOURCE_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceHandler::get);

        apiRoute.put("/datasource/resources/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_EDIT)
                                .addPermission(PermissionConstants.DATASOURCE_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceHandler::edit);

        apiRoute.delete("/datasource/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_DELETE)
                                .addPermission(PermissionConstants.DATASOURCE_DELETE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceHandler::delete);

        apiRoute.post("/datasource/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_EDIT)
                                .addPermission(PermissionConstants.DATASOURCE_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceHandler::rename);

        // 文件夹 CRUD
        apiRoute.post("/datasource/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_FOLDER_EDIT)
                                .addPermission(PermissionConstants.DATASOURCE_FOLDER_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceFolderHandler::rename);

        apiRoute.get("/datasource/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_READ)
                                .addPermission(PermissionConstants.DATASOURCE_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceFolderHandler::get);

        apiRoute.delete("/datasource/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_FOLDER_DELETE)
                                .addPermission(PermissionConstants.DATASOURCE_FOLDER_DELETE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceFolderHandler::delete);

        apiRoute.post("/datasource/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_FOLDER_CREATE)
                                .addPermission(PermissionConstants.DATASOURCE_FOLDER_CREATE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceFolderHandler::create);

        // 树形和资源列表
        apiRoute.get("/datasource/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_READ)
                                .addPermission(PermissionConstants.DATASOURCE_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceHandler::tree);

        apiRoute.get("/datasource/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_READ)
                                .addPermission(PermissionConstants.DATASOURCE_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceHandler::list);

        apiRoute.post("/datasource/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_CREATE)
                                .addPermission(PermissionConstants.DATASOURCE_CREATE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceHandler::create);

        // 表信息查询
        apiRoute.get("/datasource/resources/:resourceId/tables")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_READ)
                                .addPermission(PermissionConstants.DATASOURCE_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceHandler::getTables);

        apiRoute.get("/datasource/resources/:resourceId/tables/:tableName/columns")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.DATASOURCE_READ)
                                .addPermission(PermissionConstants.DATASOURCE_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(dataSourceHandler::getColumns);

        // 权限
        apiRoute.get("/datasource/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(dataSourceHandler::listResourcePermission);

        apiRoute.put("/datasource/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(dataSourceHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {

    }
}
