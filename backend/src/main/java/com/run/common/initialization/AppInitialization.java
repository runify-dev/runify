package com.run.common.initialization;

import com.run.common.openapi.DocRoute;
import com.run.route.*;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/27  20:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

public class AppInitialization {
    @Inject
    public AppInitialization(UserRoute userRoute,
                             NodeRoute nodeRoute,
                             KnowledgeRoute knowledgeRoute,
                             FileRoute fileRoute,
                             ChatRoute chatRoute,
                             ApplicationRoute applicationRoute,
                             DocRoute docRoute,
                             UIInitialization uiInitialization,
                             MigrationInitialization migrationInitialization) {
        userRoute.init();
        nodeRoute.init();
        knowledgeRoute.init();
        fileRoute.init();
        chatRoute.init();
        applicationRoute.init();
        docRoute.init();
        migrationInitialization.init();
        uiInitialization.init();
    }
}
