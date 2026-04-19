package com.run.dagger.module;


import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.provider.ConversationTokenProvider;
import com.run.auth.provider.TokenProvider;
import com.run.dao.mapper.*;
import dagger.Module;
import dagger.Provides;
import io.vertx.sqlclient.Pool;
import org.jooq.SQLDialect;

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
    @Inject
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
                                                          ProjectMapper projectMapper
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
                projectMapper);
        return new TokenBasicAuthHandler(tokenProvider);
    }

    @Inject
    @Singleton
    @Provides
    @Named("conversationTokenBasicAuthHandler")
    public TokenBasicAuthHandler getChatTokenBasicAuthHandler(Pool pool, SQLDialect dbType) {
        return new TokenBasicAuthHandler(new ConversationTokenProvider());
    }
}
