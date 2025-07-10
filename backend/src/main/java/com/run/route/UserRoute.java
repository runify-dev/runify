package com.run.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.run.auth.TokenBasicAuthHandler;
import com.run.common.openapi.CommonOpenAPI;
import com.run.common.route.IRoute;
import com.run.handler.user.IUserHandler;
import com.run.handler.user.pojo.LoginPojo;
import com.run.handler.user.pojo.UserPojo;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  23:50}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class UserRoute implements IRoute {
    @Inject
    @Named("apiRoute")
    protected Router apiRoute;
    @Inject
    protected OpenAPI openAPI;
    @Inject
    protected TokenBasicAuthHandler tokenBasicAuthHandler;
    @Inject
    protected IUserHandler iUserHandler;


    @Override
    public void initRoute() {
        apiRoute.get("/user")
                .handler(tokenBasicAuthHandler)
                .handler(iUserHandler.profile());

        apiRoute.post("/login")
                .handler(BodyHandler.create())
                .handler(iUserHandler.login());
    }

    @Override
    public void initOpenApi() {
        openAPI.path("/api/user", new PathItem()
                .get(new Operation().description("获取用户信息")
                        .security(CommonOpenAPI.getSecurity())
                        .addTagsItem("用户管理")
                        .responses(CommonOpenAPI.getApiResponse(UserPojo::schema)))
        );
        openAPI.path("/api/login", new PathItem().post(new Operation().description("登陆")
                .addTagsItem("用户管理")
                .requestBody(CommonOpenAPI.getApiRequest(LoginPojo::schema))
                .responses(CommonOpenAPI.getApiResponse(new StringSchema().description("令牌"))
                )));
    }


}
