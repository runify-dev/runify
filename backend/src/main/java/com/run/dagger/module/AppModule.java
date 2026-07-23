package com.run.dagger.module;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
        // 节点 nodeData 经 JsonObject.mapTo(...) 反序列化：容忍多余字段，
        // 避免前端/AI 携带 pojo 未声明的字段（如 js 参数误带 reference）导致节点构造 NPE/崩溃
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return vertx;
    }

    @Named("mainRoute")
    @Singleton
    @Provides
    public Router mainRoute() {
        return Router.router(vertx);
    }

}