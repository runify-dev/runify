package com.run.common.initialization;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.run.guice.*;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/15  22:32}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class GuiceInitialization implements Initialization {
    /**
     * 这里之所以使用List<List> 是因为下标大的注入类需要依赖下表小的类
     * 例如 UserRoute 需要依赖RouteModule 那么UserRoute就在下面
     */
    private final List<List<Class<? extends Module>>> models = List.of(
            List.of(MigrationModule.class, RouteModule.class, SqlPoolModule.class, OpenAPIModule.class),
            List.of(MapperModule.class),
            List.of(TokenAuthHandlerModule.class),
            List.of(HandlerModule.class)
    );

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Injector initialization(Injector injector) {
        for (List<Class<? extends Module>> model : models) {
            Injector finalInjector = injector;
            List<? extends Module> modules = model.stream().map(finalInjector::getInstance).toList();
            injector = injector.createChildInjector(modules);
        }
        return injector;
    }
}
