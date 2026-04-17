package com.run.auth.provider;

import com.auth0.jwt.interfaces.Claim;
import com.run.auth.constants.PermissionConstants;
import com.run.common.util.JWTUtil;
import com.run.dao.common.F;
import com.run.dao.entity.*;
import com.run.dao.mapper.*;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.sqlclient.Pool;
import org.jooq.Record1;
import org.jooq.SQLDialect;
import org.jooq.SelectConditionStep;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/4  23:50}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TokenProvider implements AuthenticationProvider {
    private Pool pool;
    static List<PermissionConstants> viewPermission = Arrays.stream(PermissionConstants.values())
            .filter(p -> p.getResourcePermissionGroups().contains(PermissionConstants.ResourcePermissionGroup.VIEW)).toList();

    static List<PermissionConstants> managePermission = Arrays.stream(PermissionConstants.values())
            .filter(p -> p.getResourcePermissionGroups().contains(PermissionConstants.ResourcePermissionGroup.MANAGE)).toList();
    private SQLDialect dbType;
    private UserMapper userMapper;
    private RoleUserRelationMapper roleUserRelationMapper;
    private RolePermissionRelationMapper rolePermissionRelationMapper;
    private RoleMapper roleBaseMapper;
    private ApplicationPermissionMapper applicationPermissionBaseMapper;
    private NotePermissionMapper notePermissionBaseMapper;
    private ModelPermissionMapper modelPermissionBaseMapper;
    private ProjectPermissionMapper projectPermissionBaseMapper;


    public TokenProvider(Pool pool, SQLDialect dbType) {
        this.pool = pool;
        this.dbType = dbType;
    }

    public TokenProvider(UserMapper userMapper, RoleUserRelationMapper roleUserRelationMapper,
                         RolePermissionRelationMapper rolePermissionRelationMapper,
                         RoleMapper roleBaseMapper,
                         ApplicationPermissionMapper applicationPermissionBaseMapper,
                         NotePermissionMapper notePermissionBaseMapper,
                         ModelPermissionMapper modelPermissionBaseMapper,
                         ProjectPermissionMapper projectPermissionBaseMapper) {
        this.userMapper = userMapper;
        this.roleBaseMapper = roleBaseMapper;
        this.rolePermissionRelationMapper = rolePermissionRelationMapper;
        this.roleUserRelationMapper = roleUserRelationMapper;
        this.applicationPermissionBaseMapper = applicationPermissionBaseMapper;
        this.notePermissionBaseMapper = notePermissionBaseMapper;
        this.modelPermissionBaseMapper = modelPermissionBaseMapper;
        this.projectPermissionBaseMapper = projectPermissionBaseMapper;


    }

    public List<String> getPermissions(List<ApplicationPermission> applicationPermissions,
                                       List<NotePermission> notePermissions,
                                       List<ModelPermission> modelPermissions,
                                       List<ProjectPermission> projectPermissions,
                                       List<RolePermissionRelation> rolePermissionRelations) {

        return Stream.of(
                buildPermissionStream(applicationPermissions, viewPermission, managePermission, rolePermissionRelations,
                        ApplicationPermission::getPermission, ApplicationPermission::getTarget),
                buildPermissionStream(notePermissions, viewPermission, managePermission, rolePermissionRelations,
                        NotePermission::getPermission, NotePermission::getTarget),
                buildPermissionStream(modelPermissions, viewPermission, managePermission, rolePermissionRelations,
                        ModelPermission::getPermission, ModelPermission::getTarget),
                buildPermissionStream(projectPermissions, viewPermission, managePermission, rolePermissionRelations,
                        ProjectPermission::getPermission, ProjectPermission::getTarget),
                rolePermissionRelations.stream().map(RolePermissionRelation::getPermissionId)
        ).flatMap(Function.identity()).distinct().toList();
    }

    private <T> Stream<String> buildPermissionStream(
            List<T> permissions,
            List<PermissionConstants> viewPermission,
            List<PermissionConstants> managePermission,
            List<RolePermissionRelation> rolePermissionRelations,
            Function<T, String> permissionGetter,
            Function<T, UUID> targetGetter
    ) {
        return permissions.stream().flatMap(item -> {
            String permission = permissionGetter.apply(item);
            String targetStr = targetGetter.apply(item).toString();

            return switch (permission) {
                case "VIEW" -> viewPermission.stream()
                        .map(p -> p.getPermission().toString(targetStr));
                case "MANAGE" -> managePermission.stream()
                        .map(p -> p.getPermission().toString(targetStr));
                case "ROLE" -> rolePermissionRelations.stream()
                        .map(p -> p.getPermissionId() + ":" + targetStr);
                default -> Stream.empty();
            };
        });
    }

    @Override
    public Future<User> authenticate(Credentials credentials) {
        TokenCredentials tokenCredentials = (TokenCredentials) credentials;
        String token = tokenCredentials.getToken();
        Map<String, Claim> data = JWTUtil.decodeToken(token);
        String userId = data.get("id").asString();
        SelectConditionStep<Record1<UUID>> where = roleUserRelationMapper.getDslContext()
                .select(F.field(RoleUserRelation::getRoleId))
                .from(roleUserRelationMapper.getTable())
                .where(F.field(RoleUserRelation::getUserId).eq(F.params(RoleUserRelation::getUserId)));
        Future<List<Role>> listFuture = roleBaseMapper.list(F.field(RoleUserRelation::getId).in(where), Map.of("userId", userId));
        return listFuture.compose(roles -> {
            List<String> roleIds = roles.stream().map(Role::getId).toList();
            Future<List<RolePermissionRelation>> rolePermissionRelations = rolePermissionRelationMapper
                    .list(F.field(RolePermissionRelation::getRoleId).in(F.params(RolePermissionRelation::getRoleId)),
                            Map.of("roleId", roleIds), List.of("roleId"));
            Future<List<ApplicationPermission>> applicationPermissionList = applicationPermissionBaseMapper.list(F.field(ApplicationPermission::getUserId).eq(F.params(ApplicationPermission::getUserId)), Map.of("userId", userId));
            Future<List<NotePermission>> notePermissionList = notePermissionBaseMapper.list(F.field(NotePermission::getUserId).eq(F.params(NotePermission::getUserId)), Map.of("userId", userId));
            Future<List<ModelPermission>> modelPermissionList = modelPermissionBaseMapper.list(F.field(ApplicationPermission::getUserId).eq(F.params(ApplicationPermission::getUserId)), Map.of("userId", userId));
            Future<List<ProjectPermission>> projectPermissionList = projectPermissionBaseMapper.list(F.field(ApplicationPermission::getUserId).eq(F.params(ApplicationPermission::getUserId)), Map.of("userId", userId));
            Future<com.run.dao.entity.User> userFuture = userMapper.getById(userId);
            return Future.all(applicationPermissionList, notePermissionList, modelPermissionList, projectPermissionList, rolePermissionRelations, userFuture).compose(r -> {
                List<String> permissions = getPermissions(r.resultAt(0), r.resultAt(1), r.resultAt(2), r.resultAt(3), r.resultAt(4));
                com.run.dao.entity.User user = r.resultAt(5);
                if (user == null) {
                    return Future.failedFuture(new RuntimeException("用户不存在"));
                }
                UserImpl result = new UserImpl(new JsonObject(Map.of("user", user, "permissions", permissions, "roles", roleIds)), new JsonObject());
                return Future.succeededFuture(result);
            });
        });

    }
}
