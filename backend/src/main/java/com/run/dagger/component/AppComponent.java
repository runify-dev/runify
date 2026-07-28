package com.run.dagger.component;

import com.run.common.initialization.AppInitialization;
import com.run.common.search.SearchClient;
import com.run.dagger.module.*;
import com.run.dao.mapper.*;
import dagger.Component;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/26  16:27}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Singleton
@Component(modules = {AppModule.class, ConfigModule.class, SqlPoolModule.class, CacheStoreModule.class, SearchModule.class, MessageQueueModule.class, RouteModule.class, OpenAPIModule.class, TokenAuthHandlerModule.class})
public interface AppComponent {
    @Named("mainRoute")
    Router mainRoute();

    AppInitialization getRouterInitialization();

    ModelMapper modelMapper();

    SkillMapper skillMapper();

    SkillFileMapper skillFileMapper();

    ToolMapper toolMapper();

    ToolFolderMapper toolFolderMapper();

    ToolRelationMapper toolRelationMapper();

    ToolPermissionMapper toolPermissionMapper();

    DatasourceMapper dataSourceMapper();

    FileMapper fileMapper();

    ConversationMessageMapper conversationMessageMapper();

    CtxFactMapper ctxFactMapper();

    CtxSummaryMapper ctxSummaryMapper();

    CtxSectionMapper ctxSectionMapper();

    SearchClient searchClient();

    Vertx vertx();
}
