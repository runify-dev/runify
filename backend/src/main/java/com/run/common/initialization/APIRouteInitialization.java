package com.run.common.initialization;

import com.google.inject.Injector;
import com.run.common.openapi.DocRoute;
import com.run.common.route.IRoute;
import com.run.common.util.ClassScanUtil;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/5/5  23:55}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class APIRouteInitialization implements Initialization {
    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public Injector initialization(Injector injector) {
        List<Class<? extends IRoute>> classList = ClassScanUtil.getClassList("com.run.route", IRoute.class);
        for (Class<? extends IRoute> aClass : classList) {
            IRoute instance = injector.getInstance(aClass);
            instance.initOpenApi();
            instance.initRoute();
        }
        DocRoute instance = injector.getInstance(DocRoute.class);
        instance.initOpenApi();
        instance.initRoute();
        return injector;
    }
}
