package com.run.auth.provider;

import com.auth0.jwt.interfaces.Claim;
import com.run.auth.constants.PermissionConstants;
import com.run.common.util.JWTUtil;
import com.run.dao.common.F;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.*;
import com.run.dao.entity.Role;
import com.run.dao.mapper.*;
import com.run.handler.common.pojo.QueryResourcePojo;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.sqlclient.Pool;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/4  23:50}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TokenProvider implements AuthenticationProvider {
    private Pool pool;
    static Map<PermissionConstants.Group, List<PermissionConstants>> viewPermission = Arrays.stream(PermissionConstants.values())
            .filter(p -> p.getResourcePermissionGroups().contains(PermissionConstants.ResourcePermissionGroup.VIEW))
            .collect(Collectors.groupingBy(p -> p.getPermission().getGroup()));

    static Map<PermissionConstants.Group, List<PermissionConstants>> managePermission = Arrays.stream(PermissionConstants.values())
            .filter(p -> p.getResourcePermissionGroups().contains(PermissionConstants.ResourcePermissionGroup.MANAGE))
            .collect(Collectors.groupingBy(p -> p.getPermission().getGroup()));

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
    Map<PermissionConstants.ResourcePermissionGroup, String> view =
            Map.of(PermissionConstants.ResourcePermissionGroup.NOT_AUTH, "#{permissionNotAuth}",
                    PermissionConstants.ResourcePermissionGroup.VIEW, "#{permissionView}",
                    PermissionConstants.ResourcePermissionGroup.MANAGE, "#{permissionManage}",
                    PermissionConstants.ResourcePermissionGroup.ROLE, "#{permissionRole}");


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
                         ProjectMapper projectMapper) {
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
    }

    public <Permission extends BaseEntity<Permission>,
            PermissionMapper extends BaseMapper<Permission>,
            Relation extends BaseEntity<Relation>,
            RelationMapper extends BaseMapper<Relation>>
    Condition getWhereByPermission(PermissionMapper permissionMapper,
                                   RelationMapper relationMapper,
                                   PermissionConstants.ResourcePermissionGroup resourcePermissionGroup) {
        Field<Object> ancestorId = DSL.field("ancestor_id");
        Field<Object> descendantId = DSL.field("descendant_id");
        Field<Object> permissionField = DSL.field("permission");
        Field<Object> userIdField = DSL.field("user_id");
        Field<Object> targetField = DSL.field("target");
        var currentTargets = permissionMapper.getDslContext()
                .select(targetField)
                .from(permissionMapper.getTable())
                .where(permissionField.eq(DSL.param("permission", DSL.param(view.get(resourcePermissionGroup)))))
                .and(userIdField.eq(DSL.param("#{userId}")));
        var otherTargets = permissionMapper.getDslContext()
                .select(targetField)
                .from(permissionMapper.getTable())
                .where(permissionField.in(
                        view.entrySet().stream()
                                .filter(e -> e.getKey() != resourcePermissionGroup)
                                .map(Map.Entry::getValue).map(DSL::param)
                                .toList()
                ))
                .and(userIdField.eq(DSL.param("#{userId}")));

        var otherDescendants = relationMapper.getDslContext()
                .select(descendantId)
                .from(relationMapper.getTable())
                .where(ancestorId.in(otherTargets));

        return ancestorId.in(currentTargets).and(descendantId.notIn(otherDescendants));


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

    public <Permission extends BaseEntity<Permission>,
            PermissionMapper extends BaseMapper<Permission>,
            Relation extends BaseEntity<Relation>,
            RelationMapper extends BaseMapper<Relation>,
            Resource extends BaseEntity<Resource>,
            ResourceMapper extends BaseMapper<Resource>>
    Future<List<String>> getResourcePermission(ResourceMapper resourceMapper,
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

            Stream<String> viewPermissions = viewPermission.get(group).stream().flatMap(permissionConstants -> v.stream().map(getId)
                    .map(id -> permissionConstants.getPermission().toString(id.toString())));
            Stream<String> managePermissions = managePermission.get(group).stream().flatMap(permissionConstants -> m.stream().map(getId)
                    .map(id -> permissionConstants.getPermission().toString(id.toString())));
            Stream<String> roleStream = rolePermissionRelations
                    .stream()
                    .filter(f -> f.getPermissionId().startsWith(group.name()))
                    .flatMap(rolePermissionRelation -> r.stream().map(getId).map(id -> rolePermissionRelation.getPermissionId() + ":" + id));
            List<String> _r = Stream.of(viewPermissions, managePermissions, roleStream).flatMap(item -> item).toList();
            return Future.succeededFuture(_r);
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
            return rolePermissionRelations.compose(rolePermissionRelationResult -> {
                Future<List<String>> applicationPermissions =
                        getResourcePermission(
                                applicationMapper,
                                applicationPermissionBaseMapper,
                                applicationRelationMapper,
                                userId,
                                ApplicationRelation::getDescendantId,
                                rolePermissionRelationResult,
                                PermissionConstants.Group.APPLICATION);
                Future<List<String>> notePermissions =
                        getResourcePermission(
                                noteMapper,
                                notePermissionBaseMapper,
                                noteRelationMapper,
                                userId,
                                NoteRelation::getDescendantId,
                                rolePermissionRelationResult,
                                PermissionConstants.Group.NOTE);
                Future<List<String>> moelPermissions =
                        getResourcePermission(
                                modelMapper,
                                modelPermissionBaseMapper,
                                modelRelationMapper,
                                userId,
                                ModelRelation::getDescendantId,
                                rolePermissionRelationResult,
                                PermissionConstants.Group.MODEL);
                Future<com.run.dao.entity.User> userFuture = userMapper.getById(userId);
                Future<List<String>> projectPermissions =
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
                List<String> applicationPermissions = result.resultAt(0);
                List<String> notePermissions = result.resultAt(1);
                List<String> modelPermissions = result.resultAt(2);
                List<String> projectPermissions = result.resultAt(3);
                List<RolePermissionRelation> permissionRelations = result.resultAt(4);
                com.run.dao.entity.User user = result.resultAt(5);
                if (user == null) {
                    return Future.failedFuture(new RuntimeException("用户不存在"));
                }
                List<String> permissions = Stream.of(applicationPermissions, notePermissions, modelPermissions, projectPermissions).flatMap(Collection::stream).toList();
                UserImpl userImpl = new UserImpl(new JsonObject(Map.of("user", user, "permissions", permissions, "roles", roleIds)), new JsonObject());
                return Future.succeededFuture(userImpl);
            });
        });

    }
}
