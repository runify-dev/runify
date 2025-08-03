package com.run;


import com.run.dagger.component.AppComponent;
import com.run.dagger.component.DaggerAppComponent;
import com.run.dagger.module.AppModule;
import com.run.dagger.module.ConfigModule;
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
    public static AppComponent appComponent;

    @Override
    public void start() {
        appComponent = DaggerAppComponent.builder()
                .configModule(new ConfigModule("/opt/runify/conf/runify.yaml"))
                .appModule(new AppModule(vertx))
                .build();
        appComponent.getRouterInitialization();
        Router router = appComponent.mainRoute();
        vertx
                .createHttpServer()
                .requestHandler(router)
                .listen(8080);
    }


    public static void main(String[] args) {
        VertxApplication.main(new String[]{RunApplication.class.getName()});
    }

}