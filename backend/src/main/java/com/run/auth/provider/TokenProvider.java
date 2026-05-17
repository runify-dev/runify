package com.run.auth.provider;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.type.TypeReference;
import com.run.auth.constants.PermissionConstants;
import com.run.auth.constants.PermissionDataConstants;
import com.run.auth.dto.TokenDTO;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.util.CommonUtils;
import com.run.common.util.JWTUtil;
import com.run.common.util.PermissionHexUtils;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.*;
import com.run.dao.entity.Role;
import com.run.dao.mapper.*;
import com.run.sql.DSL;
import com.run.sql.condition.Condition;
import com.run.sql.dialect.SQLDialect;
import com.run.sql.model.Field;
import com.run.sql.query.SelectQuery;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.sqlclient.Pool;


import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.run.sql.DSL.field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/4  23:50}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TokenProvider implements AuthenticationProvider {
    private Pool pool;

    private SQLDialect dbType;
    private UserMapper userMapper;
    private RoleUserRelationMapper roleUserRelationMapper;
    private RolePermissionRelationMapper rolePermissionRelationMapper;
    private RoleMapper roleBaseMapper;
    private ApplicationPermissionMapper applicationPermissionBaseMapper;
    private ApplicationRelationMapper applicationRelationMapper;
    private NotePermissionMapper notePermissionBaseMapper;
    private ModelPermissionMapper modelPermissionBaseMapper;
    private ProjectPermissionMapper projectPermissionBaseMapper;
    private NoteRelationMapper noteRelationMapper;
    private ModelRelationMapper modelRelationMapper;
    private ProjectRelationMapper projectRelationMapper;
    private ApplicationMapper applicationMapper;
    private NoteMapper noteMapper;
    private ModelMapper modelMapper;
    private ProjectMapper projectMapper;
    private CacheStore cacheStore;

    final static Map<PermissionConstants.ResourcePermissionGroup, String> view =
            Map.of(PermissionConstants.ResourcePermissionGroup.NOT_AUTH, "permissionNotAuth",
                    PermissionConstants.ResourcePermissionGroup.VIEW, "permissionView",
                    PermissionConstants.ResourcePermissionGroup.MANAGE, "permissionManage",
                    PermissionConstants.ResourcePermissionGroup.ROLE, "permissionRole");


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
                         ProjectPermissionMapper projectPermissionBaseMapper,
                         ApplicationRelationMapper applicationRelationMapper,
                         NoteRelationMapper noteRelationMapper,
                         ModelRelationMapper modelRelationMapper,
                         ProjectRelationMapper projectRelationMapper,
                         ApplicationMapper applicationMapper,
                         NoteMapper noteMapper,
                         ModelMapper modelMapper,
                         ProjectMapper projectMapper,
                         CacheStore cacheStore) {
        this.userMapper = userMapper;
        this.roleBaseMapper = roleBaseMapper;
        this.rolePermissionRelationMapper = rolePermissionRelationMapper;
        this.roleUserRelationMapper = roleUserRelationMapper;
        this.applicationPermissionBaseMapper = applicationPermissionBaseMapper;
        this.notePermissionBaseMapper = notePermissionBaseMapper;
        this.modelPermissionBaseMapper = modelPermissionBaseMapper;
        this.projectPermissionBaseMapper = projectPermissionBaseMapper;
        this.applicationRelationMapper = applicationRelationMapper;
        this.noteRelationMapper = noteRelationMapper;
        this.modelRelationMapper = modelRelationMapper;
        this.projectRelationMapper = projectRelationMapper;
        this.applicationMapper = applicationMapper;
        this.noteMapper = noteMapper;
        this.modelMapper = modelMapper;
        this.projectMapper = projectMapper;
        this.cacheStore = cacheStore;
    }

    public static <Permission extends BaseEntity<Permission>,
            PermissionMapper extends BaseMapper<Permission>,
            Relation extends BaseEntity<Relation>,
            RelationMapper extends BaseMapper<Relation>>
    Condition getWhereByPermission(PermissionMapper permissionMapper,
                                   RelationMapper relationMapper,
                                   PermissionConstants.ResourcePermissionGroup resourcePermissionGroup) {
        Field<Object> ancestorId = field("ancestor_id");
        Field<Object> descendantId = field("descendant_id");
        Field<Object> permissionField = field("permission");
        Field<Object> userIdField = field("user_id");
        Field<Object> targetField = field("target");
        var currentTargets = permissionMapper.getDslContext()
                .select(targetField)
                .from(permissionMapper.getTable())
                .where(permissionField.eq(DSL.param("permission", DSL.param(view.get(resourcePermissionGroup)))))
                .and(userIdField.eq(DSL.param("userId")));
        var otherTargets = permissionMapper.getDslContext()
                .select(targetField)
                .from(permissionMapper.getTable())
                .where(permissionField.in(
                        view.entrySet().stream()
                                .filter(e -> e.getKey() != resourcePermissionGroup)
                                .map(Map.Entry::getValue).map(DSL::param)
                                .toList()
                ))
                .and(userIdField.eq(DSL.param("userId")));

        var otherDescendants = relationMapper.getDslContext()
                .select(descendantId)
                .from(relationMapper.getTable())
                .where(ancestorId.in(otherTargets));

        return ancestorId.in(currentTargets).and(descendantId.notIn(otherDescendants));


    }


    public <Permission extends BaseEntity<Permission>,
            PermissionMapper extends BaseMapper<Permission>,
            Relation extends BaseEntity<Relation>,
            RelationMapper extends BaseMapper<Relation>,
            Resource extends BaseEntity<Resource>,
            ResourceMapper extends BaseMapper<Resource>>
    Future<Map<String, Long>> getResourcePermission(ResourceMapper resourceMapper,
                                                    PermissionMapper permissionMapper,
                                                    RelationMapper relationMapper,
                                                    String userId,
                                                    Function<Relation, UUID> getId,
                                                    List<RolePermissionRelation> rolePermissionRelations,
                                                    PermissionConstants.Group group) {
        Future<List<Relation>> view = relationMapper.list(getWhereByPermission(permissionMapper, relationMapper, PermissionConstants.ResourcePermissionGroup.VIEW),
                Map.of("userId", userId, "permissionView", "VIEW",
                        "permissionManage", "MANAGE",
                        "permissionNotAuth", "NOT_AUTH",
                        "permissionRole", "ROLE"));
        Future<List<Relation>> manage = relationMapper.list(getWhereByPermission(permissionMapper, relationMapper, PermissionConstants.ResourcePermissionGroup.MANAGE),
                Map.of("userId", userId, "permissionView", "VIEW",
                        "permissionManage", "MANAGE",
                        "permissionNotAuth", "NOT_AUTH",
                        "permissionRole", "ROLE"));

        Future<List<Relation>> role = relationMapper.list(getWhereByPermission(permissionMapper, relationMapper, PermissionConstants.ResourcePermissionGroup.ROLE),
                Map.of("userId", userId, "permissionView", "VIEW",
                        "permissionManage", "MANAGE",
                        "permissionNotAuth", "NOT_AUTH",
                        "permissionRole", "ROLE"));

        return Future.all(view, manage, role).compose(result -> {
            List<Relation> v = result.resultAt(0);
            List<Relation> m = result.resultAt(1);
            List<Relation> r = result.resultAt(2);
            HashMap<String, Long> permissionMap = new HashMap<>();
            for (Relation relation : v) {
                UUID apply = getId.apply(relation);
                permissionMap.put(group.name() + ":" + apply.toString(), PermissionDataConstants.viewPermissionBit.get(group));
            }
            for (Relation relation : m) {
                UUID apply = getId.apply(relation);
                permissionMap.put(group.name() + ":" + apply.toString(), PermissionDataConstants.managePermissionBit.get(group));
            }
            for (Relation relation : r) {
                UUID apply = getId.apply(relation);
                Long reduce = rolePermissionRelations.stream()
                        .map(RolePermissionRelation::getPermissionId)
                        .map(PermissionDataConstants.permissionMap::get)
                        .filter(p -> p.getGroup() == group)
                        .map(PermissionConstants.Permission::bit).reduce(0L, (x, y) -> x | y);
                permissionMap.put(group.name() + ":" + apply.toString(), reduce);

            }
            return Future.succeededFuture(permissionMap);
        });
    }


    @Override
    public Future<User> authenticate(Credentials credentials) {
        TokenCredentials tokenCredentials = (TokenCredentials) credentials;
        String token = tokenCredentials.getToken();
        TokenDTO tokenDTO = TokenDTO.newInstance(token);
        String userId = tokenDTO.getId();
        CompletableFuture<Optional<com.run.dao.entity.User>> userFuture = cacheStore
                .get("user:" + userId, com.run.dao.entity.User.class)
                .thenCompose(ok -> {
                    if (ok.isEmpty()) {
                        return userMapper.getById(userId)
                                .compose(user -> Future.succeededFuture(Optional.ofNullable(user))
                                ).toCompletionStage();
                    } else {
                        return CompletableFuture.completedFuture(ok);
                    }
                })
                .toCompletableFuture();
        // 获取role
        CompletableFuture<List<Role>> rolesFuture = cacheStore.get("roles:" + userId, new TypeReference<List<Role>>() {
        }).thenCompose(result -> {
            if (result.isEmpty()) {
                SelectQuery where = roleUserRelationMapper.getDslContext()
                        .select(field(RoleUserRelation::getRoleId))
                        .from(roleUserRelationMapper.getTable())
                        .where(field(RoleUserRelation::getUserId).eq(userId));
                return roleBaseMapper.list(field(Role::getId).in(where), Map.of())
                        .compose(roles -> Future.fromCompletionStage(cacheStore.set("roles:" + userId, roles)
                                .thenCompose(ok -> CompletableFuture.completedFuture(roles)))).toCompletionStage();
            } else {
                return CompletableFuture.completedFuture(result.get());
            }
        }).toCompletableFuture();

        CompletableFuture<Optional<Map<String, String>>> permissionsFuture = cacheStore
                .hgetall("permissions:" + userId, String.class)
                .toCompletableFuture();

        CompletableFuture<User> userCompletableFuture = CompletableFuture.allOf(
                userFuture,
                rolesFuture,
                permissionsFuture
        ).thenCompose(v -> {
            com.run.dao.entity.User user = userFuture.join()
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            List<Role> roles = rolesFuture.join();
            return permissionsFuture.thenCompose(permissions -> {
                if (permissions.isEmpty()) {
                    return getPermissionsByRoles(roles, userId)
                            .compose(ps ->
                                    Future.fromCompletionStage(cacheStore.hset("permissions:" + userId, PermissionHexUtils.toHexMap(ps))
                                            .thenCompose(ok -> CompletableFuture.completedFuture(ps))))
                            .toCompletionStage();
                }
                return CompletableFuture.completedFuture(permissions.map(PermissionHexUtils::fromHexMap).get());
            }).thenCompose(permissions -> {
                UserProfile userProfile = new UserProfile();
                CommonUtils.copyProperties(user, userProfile);
                userProfile.setPermissions(permissions);
                userProfile.setRoles(roles);
                if (PermissionConstants.Role.ADMIN.name().equals(user.getRole())) {
                    roles.add(new Role(PermissionConstants.Role.ADMIN.name(), "管理员", true, PermissionConstants.Role.ADMIN, null, null));
                }
                User userImpl = new UserImpl(new JsonObject(Map.of("user", userProfile)), new JsonObject());
                return CompletableFuture.completedFuture(userImpl);
            });
        });
        return Future.fromCompletionStage(userCompletableFuture);
    }

    public Future<Map<String, Long>> getPermissionsByRoles(List<Role> roles, String userId) {
        List<String> roleIds = roles.stream().map(Role::getId).toList();
        Future<List<RolePermissionRelation>> rolePermissionRelations = rolePermissionRelationMapper
                .list(field(RolePermissionRelation::getRoleId).in(roleIds),
                        Map.of("roleId", roleIds));
        return rolePermissionRelations.compose(rolePermissionRelationResult -> {
            Future<Map<String, Long>> applicationPermissions =
                    getResourcePermission(
                            applicationMapper,
                            applicationPermissionBaseMapper,
                            applicationRelationMapper,
                            userId,
                            ApplicationRelation::getDescendantId,
                            rolePermissionRelationResult,
                            PermissionConstants.Group.APPLICATION);
            Future<Map<String, Long>> notePermissions =
                    getResourcePermission(
                            noteMapper,
                            notePermissionBaseMapper,
                            noteRelationMapper,
                            userId,
                            NoteRelation::getDescendantId,
                            rolePermissionRelationResult,
                            PermissionConstants.Group.NOTE);
            Future<Map<String, Long>> moelPermissions =
                    getResourcePermission(
                            modelMapper,
                            modelPermissionBaseMapper,
                            modelRelationMapper,
                            userId,
                            ModelRelation::getDescendantId,
                            rolePermissionRelationResult,
                            PermissionConstants.Group.MODEL);
            Future<com.run.dao.entity.User> userFuture = userMapper.getById(userId);
            Future<Map<String, Long>> projectPermissions =
                    getResourcePermission(
                            projectMapper,
                            projectPermissionBaseMapper,
                            projectRelationMapper,
                            userId,
                            ProjectRelation::getDescendantId,
                            rolePermissionRelationResult,
                            PermissionConstants.Group.PROJECT);
            return Future.all(applicationPermissions, notePermissions, moelPermissions, projectPermissions, rolePermissionRelations, userFuture);
        }).compose(result -> {
            Map<String, Long> applicationPermissions = result.resultAt(0);
            Map<String, Long> notePermissions = result.resultAt(1);
            Map<String, Long> modelPermissions = result.resultAt(2);
            Map<String, Long> projectPermissions = result.resultAt(3);
            List<RolePermissionRelation> permissionRelations = result.resultAt(4);

            Map<String, Long> menuPermissionMap = permissionRelations.stream()
                    .map(RolePermissionRelation::getPermissionId)
                    .map(PermissionDataConstants.permissionMap::get)
                    .collect(Collectors.toMap(PermissionConstants.Permission::toString, PermissionConstants.Permission::bit));


            Map<String, Long> r = Stream.of(applicationPermissions, notePermissions, modelPermissions, projectPermissions, menuPermissionMap)
                    .flatMap(map -> map.entrySet().stream())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (v1, v2) -> v1
                    ));
            return Future.succeededFuture(r);
        });
    }

}
