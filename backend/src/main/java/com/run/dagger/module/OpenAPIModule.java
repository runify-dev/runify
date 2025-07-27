package com.run.dagger.module;


import dagger.Module;
import dagger.Provides;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;

import javax.inject.Singleton;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/27  21:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class OpenAPIModule {
    @Provides
    @Singleton
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.openapi("3.0.3");
        openAPI.info(new Info()
                .title("MyKB API")
                .description("我的知识库平台")
                .version("1.0.0")
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html")));
        openAPI.components(new Components().addSecuritySchemes("tokenAuth",
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
        ).addSecuritySchemes("AnonymousAuth", new SecurityScheme()));
        return openAPI;
    }

}
