package com.run.dagger.module;


import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.provider.ConversationTokenProvider;
import com.run.auth.provider.TokenProvider;
import com.run.common.cache.CacheStore;
import com.run.dao.mapper.*;
import com.run.sql.dialect.SQLDialect;
import dagger.Module;
import dagger.Provides;
import io.vertx.sqlclient.Pool;


import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class TokenAuthHandlerModule {

    @Singleton
    @Provides
    @Named("tokenBasicAuthHandler")
    public TokenBasicAuthHandler getTokenBasicAuthHandler(UserMapper userMapper, RoleUserRelationMapper roleUserRelationMapper,
                                                          RolePermissionRelationMapper rolePermissionRelationMapper,
                                                          RoleMapper roleBaseMapper,
                                                          ApplicationPermissionMapper applicationPermissionBaseMapper,
                                                          NotePermissionMapper notePermissionBaseMapper,
                                                          ModelPermissionMapper modelPermissionBaseMapper,
                                                          ProjectPermissionMapper projectPermissionBaseMapper,
                                                          ApplicationRelationMapper applicationRelationMapper,
                                                          NoteRelationMapper noteRelationMapper,
                                                          ModelRelationMapper modelRelationMapper,
                                                          ProjectRelationMapper projectRelationMapper,
                                                          ApplicationMapper applicationMapper,
                                                          NoteMapper noteMapper,
                                                          ModelMapper modelMapper,
                                                          ProjectMapper projectMapper,
                                                          CacheStore cacheStore
    ) {
        TokenProvider tokenProvider = new TokenProvider(userMapper,
                roleUserRelationMapper,
                rolePermissionRelationMapper, roleBaseMapper, applicationPermissionBaseMapper,
                notePermissionBaseMapper,
                modelPermissionBaseMapper, projectPermissionBaseMapper,
                applicationRelationMapper,
                noteRelationMapper,
                modelRelationMapper,
                projectRelationMapper,
                applicationMapper,
                noteMapper,
                modelMapper,
                projectMapper,
                cacheStore);
        return new TokenBasicAuthHandler(tokenProvider);
    }

    @Inject
    @Singleton
    @Provides
    @Named("conversationTokenBasicAuthHandler")
    public TokenBasicAuthHandler getChatTokenBasicAuthHandler(
            ApplicationPermissionMapper applicationPermissionBaseMapper,
            ApplicationRelationMapper applicationRelationMapper,
            ApplicationMapper applicationMapper,
            RoleMapper roleMapper,
            RoleUserRelationMapper roleUserRelationMapper,
            CacheStore cacheStore) {
        return new TokenBasicAuthHandler(new ConversationTokenProvider(applicationMapper, applicationRelationMapper, applicationPermissionBaseMapper, roleMapper, roleUserRelationMapper, cacheStore));
    }
}
