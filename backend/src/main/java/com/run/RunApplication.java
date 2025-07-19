package com.run;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.run.common.initialization.Initialization;
import com.run.guice.AppModule;
import com.run.guice.PropertiesModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.launcher.application.VertxApplication;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/12  21:12}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class RunApplication extends AbstractVerticle {
    static Injector injector;

    @Override
    public void start() {
        injector = Guice.createInjector(
                new AppModule(vertx),
                new PropertiesModule("/opt/run/conf/run.properties")
        );
        injector = Initialization.init(injector);
        Router mainRoute = injector.getInstance(Key.get(Router.class, Names.named("mainRoute")));
        vertx
                .createHttpServer()
                .requestHandler(mainRoute)
                .listen(8080);
    }


    public static void main(String[] args) {
        VertxApplication.main(new String[]{RunApplication.class.getName()});
    }

}