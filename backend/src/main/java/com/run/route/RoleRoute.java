package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.role.IRoleHandler;
import com.run.handler.role.impl.RoleHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

public class RoleRoute implements IRoute {
    protected final Router apiRoute;

    protected final TokenBasicAuthHandler tokenBasicAuthHandler;

    private final IRoleHandler roleHandler;


    protected OpenAPI openAPI;

    @Inject
    public RoleRoute(@Named("apiRoute") Router apiRoute,
                     OpenAPI openAPI,
                     @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                     RoleHandlerImpl roleHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
        this.roleHandler = roleHandler;
    }

    @Override
    public void initRoute() {
        apiRoute.get("/role")
                .handler(tokenBasicAuthHandler)
                .handler(roleHandler::query);

        apiRoute.delete("/role/:roleId")
                .handler(tokenBasicAuthHandler)
                .handler(roleHandler::delete);

        apiRoute.post("/role")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(roleHandler::create);

        apiRoute.post("/role/:roleId/user")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(roleHandler::addUser);

        apiRoute.get("/role/:roleId/permissions")
                .handler(tokenBasicAuthHandler)
                .handler(roleHandler::permissions);

        apiRoute.post("/role/:roleId/permissions")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(roleHandler::modifyPermissions);
    }

    @Override
    public void initOpenApi() {

    }
}
