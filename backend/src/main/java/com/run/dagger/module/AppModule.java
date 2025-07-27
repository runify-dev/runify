package com.run.dagger.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.Router;

import javax.inject.Named;
import javax.inject.Singleton;
import java.text.SimpleDateFormat;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/12  23:08}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class AppModule {
    private final Vertx vertx;

    public AppModule(Vertx vertx) {
        this.vertx = vertx;
    }


    @Singleton
    @Provides
    public Vertx vertx() {
        ObjectMapper mapper = DatabindCodec.mapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        return vertx;
    }

    @Named("mainRoute")
    @Singleton
    @Provides
    public Router mainRoute() {
        return Router.router(vertx);
    }

}