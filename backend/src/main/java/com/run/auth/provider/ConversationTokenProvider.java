package com.run.auth.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.auth.constants.PermissionConstants;
import com.run.auth.constants.TokenTypeConstants;
import com.run.auth.dto.TokenDTO;
import com.run.common.cache.CacheStore;
import com.run.dao.entity.Application;
import com.run.dao.mapper.*;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.QueryResourcePojo;
import com.run.handler.conversation.vo.ApplicationQueryVO;
import com.run.sql.DSL;
import com.run.sql.condition.Condition;
import com.run.sql.query.SelectQuery;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.UserImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.*;
import java.util.concurrent.CompletionStage;

import static com.run.common.util.ConditionCommonUtil.isApplicationRead;
import static com.run.sql.DSL.field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/10  16:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ConversationTokenProvider implements AuthenticationProvider {
    private final CacheStore cacheStore;
    private final ApplicationMapper applicationMapper;
    private final ApplicationRelationMapper applicationRelationMapper;
    private final ApplicationPermissionMapper applicationPermissionMapper;
    private final RoleMapper roleMapper;
    private final RoleUserRelationMapper roleUserRelationMapper;

    public ConversationTokenProvider(
            ApplicationMapper applicationMapper,
            ApplicationRelationMapper applicationRelationMapper,
            ApplicationPermissionMapper applicationPermissionBaseMapper,
            RoleMapper roleMapper,
            RoleUserRelationMapper roleUserRelationMapper,
            CacheStore cacheStore) {
        this.applicationMapper = applicationMapper;
        this.applicationPermissionMapper = applicationPermissionBaseMapper;
        this.applicationRelationMapper = applicationRelationMapper;
        this.cacheStore = cacheStore;
        this.roleMapper = roleMapper;
        this.roleUserRelationMapper = roleUserRelationMapper;
    }

    private Future<Condition> getConditionAsync(String userId) {
        return isApplicationRead(applicationPermissionMapper, roleMapper, roleUserRelationMapper, userId).map(isRead -> {
            Condition condition = ResourceHandlerImpl.getWhereByPermission(applicationPermissionMapper, applicationRelationMapper, new QueryResourcePojo(), isRead);
            return condition.or(field(Application::getAllowAnonymousAccess).eq(Boolean.TRUE));
        });
    }

    public Future<List<String>> getApplicationIds(TokenDTO tokenDTO) {
        Future<List<Application>> a =
                applicationMapper.list(DSL.field(Application::getAllowAnonymousAccess).eq(Boolean.TRUE));

        if (tokenDTO.getType().equals(TokenTypeConstants.USER)) {
            Future<Condition> conditionFuture = getConditionAsync(tokenDTO.getId());
            Future<List<Application>> auth = conditionFuture.compose(c -> {
                return applicationMapper.list(c, Map.of("userId", tokenDTO.getId(), "permissionView", "VIEW",
                        "permissionManage", "MANAGE",
                        "permissionNotAuth", "NOT_AUTH",
                        "permissionRole", "ROLE"));
            });
            a = Future.all(a, auth).compose(composite -> {
                List<Application> permissionList = composite.resultAt(0);
                List<Application> anonymousList = composite.resultAt(1);
                List<Application> result = new ArrayList<>(permissionList);
                result.addAll(anonymousList);
                return Future.succeededFuture(result);
            });
        }
        return a.compose(applications -> Future.succeededFuture(applications.stream().map(Application::getId).map(UUID::toString).toList()));
    }

    public Future<List<String>> getCacheApplicationIds(TokenDTO tokenDTO) {

        CompletionStage<Optional<List<String>>> cache = this.cacheStore.get("c::" + tokenDTO.getId(), new TypeReference<List<String>>() {
        });
        return Future.fromCompletionStage(cache.thenCompose(applicationIdList -> {
            if (applicationIdList.isEmpty()) {
                return getApplicationIds(tokenDTO).toCompletionStage();
            }
            return Future.succeededFuture(applicationIdList.get()).toCompletionStage();
        }));

    }

    @Override
    public Future<User> authenticate(Credentials credentials) {
        TokenCredentials tokenCredentials = (TokenCredentials) credentials;
        String token = tokenCredentials.getToken();
        TokenDTO tokenDTO = TokenDTO.newInstance(token);
        return getCacheApplicationIds(tokenDTO).compose(applicationIds -> {
            return Future.succeededFuture(new UserImpl(new JsonObject(Map.of(
                    "type", tokenDTO.getType(),
                    "id", tokenDTO.getId(),
                    "conversationUserId", tokenDTO.getId(),
                    "conversationUserType", tokenDTO.getType(),
                    "applicationIds", applicationIds
            )), new JsonObject()));
        });
    }
}
