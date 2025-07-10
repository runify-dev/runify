package com.run.guice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;

import java.text.SimpleDateFormat;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/12  23:08}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class AppModule extends AbstractModule {
    private final Vertx vertx;

    public AppModule(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    protected void configure() {
        ObjectMapper mapper = DatabindCodec.mapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        bind(Key.get(Vertx.class)).toInstance(this.vertx);
    }
}