package com.run.handler.role.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.query.Query;
import com.run.common.result.Page;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.I18n;
import com.run.dao.common.F;
import com.run.dao.entity.Role;
import com.run.dao.entity.RolePermissionRelation;
import com.run.dao.entity.RoleUserRelation;
import com.run.dao.entity.User;
import com.run.dao.mapper.RoleMapper;
import com.run.dao.mapper.RolePermissionRelationMapper;
import com.run.dao.mapper.RoleUserRelationMapper;
import com.run.dao.mapper.UserMapper;
import com.run.handler.role.IRoleHandler;
import com.run.handler.role.dto.PermissionDTO;
import com.run.handler.role.vo.*;
import com.run.handler.user.dto.UserDTO;
import com.run.handler.user.vo.UserQueryVO;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoleHandlerImpl implements IRoleHandler {
    private RoleMapper roleMapper;
    private RolePermissionRelationMapper rolePermissionRelationMapper;
    private RoleUserRelationMapper roleUserRelationMapper;
    private UserMapper userMapper;
    private CacheStore cacheStore;

    @Inject
    public RoleHandlerImpl(RoleMapper roleMapper,
                           RolePermissionRelationMapper rolePermissionRelationMapper,
                           RoleUserRelationMapper roleUserRelationMapper,
                           UserMapper userMapper,
                           CacheStore cacheStore) {
        this.roleMapper = roleMapper;
        this.rolePermissionRelationMapper = rolePermissionRelationMapper;
        this.roleUserRelationMapper = roleUserRelationMapper;
        this.userMapper = userMapper;
        this.cacheStore = cacheStore;
    }


    @Override
    public void query(RoutingContext context) {
        RoleQueryVO query = Query.format(RoleQueryVO.class, context);
        Condition condition = DSL.noCondition();
        if (StringUtils.isNotEmpty(query.getName())) {
            condition.and(F.field(Role::getName).like(F.params(Role::getName)));
        }
        if (query.getCurrentPage() != null && query.getPageSize() != null) {
            roleMapper.page(condition, query.getCurrentPage(), query.getPageSize(), CommonUtils.ofNullable("name", query.getName()))
                    .onSuccess(result -> {
                        context.end(Result.success(result).toBuffer());
                    }).onFailure(context::fail);
        } else {
            roleMapper.list(condition,
                            CommonUtils.ofNullable("name", query.getName()))
                    .onSuccess(result -> {
                        context.end(Result.success(result).toBuffer());
                    }).onFailure(context::fail);
        }
    }

    @Override
    public void create(RoutingContext context) {
        CreateRoleVO createRoleVO = context.body().asPojo(CreateRoleVO.class);
        Role role = new Role(CommonUtils.uuid7().toString(), createRoleVO.getName(), Boolean.FALSE, createRoleVO.getType(), LocalDateTime.now(), LocalDateTime.now());
        roleMapper.save(role).
                onSuccess(ok -> {
                    context.end(Result.success(role).toBuffer());
                }).onFailure(context::fail);

    }

    @Override
    public void delete(RoutingContext context) {
        String roleId = context.pathParam("roleId");
        roleMapper.deleteById(roleId)
                .onSuccess(_ -> {
                    context.end(Result.success(Boolean.TRUE).toBuffer());
                }).onFailure(context::fail);

    }

    @Override
    public void addUser(RoutingContext context) {
        AddUserVO addUserVO = context.body().asPojo(AddUserVO.class);
        List<String> userIds = addUserVO.getUserIds();
        String roleId = context.pathParam("roleId");
        List<RoleUserRelation> list = userIds.stream().distinct().map(userId -> {
            return new RoleUserRelation(CommonUtils.uuid7(), roleId, UUID.fromString(userId));
        }).toList();
        roleUserRelationMapper.batch_save(list).onSuccess(_ -> {
            context.end(Result.success(list).toBuffer());
        }).onFailure(context::fail);

    }

    @Override
    public void permissions(RoutingContext context) {
        Locale locale = context.get("locale");
        String roleId = context.pathParam("roleId");
        if (Arrays.stream(PermissionConstants.Role.values()).anyMatch(r -> r.name().equals(roleId))) {
            List<PermissionDTO> permissionList = getPermissionList(locale, roleId, Boolean.TRUE);
            context.end(Result.success(permissionList).toBuffer());
        } else {
            Future.all(roleMapper.getById(roleId),
                            rolePermissionRelationMapper.list(F.field(RolePermissionRelation::getRoleId).eq(F.params(RolePermissionRelation::getRoleId)),
                                    Map.of("roleId", roleId)))
                    .onSuccess(result -> {
                        Role role = result.resultAt(0);
                        List<RolePermissionRelation> rolePermissionRelations = result.resultAt(1);
                        List<PermissionDTO> allPermissions = getPermissionList(locale, role.getType().name(), Boolean.FALSE);
                        Set<String> selectedPermissionIds = rolePermissionRelations.stream()
                                .map(RolePermissionRelation::getPermissionId)
                                .collect(Collectors.toSet());
                        allPermissions.forEach(permission ->
                                permission.setSelected(selectedPermissionIds.contains(permission.getPermission()))
                        );
                        context.end(Result.success(allPermissions).toBuffer());
                    }).onFailure(context::fail);
        }
    }

    @Override
    public void modifyPermissions(RoutingContext context) {
        UserProfile user = context.user().get("user");
        String roleId = context.pathParam("roleId");
        ModifyPermissionsVO modifyPermissionsVO = context.body().asPojo(ModifyPermissionsVO.class);
        List<String> permissions = modifyPermissionsVO.getPermissions();
        rolePermissionRelationMapper.delete(F.field(RolePermissionRelation::getRoleId).eq(F.params(RolePermissionRelation::getRoleId)), Map.of("roleId", roleId))
                .compose(ok -> {
                    List<RolePermissionRelation> list = permissions.stream().map(permission -> {
                        return new RolePermissionRelation(CommonUtils.uuid7(), roleId, permission);
                    }).toList();
                    return rolePermissionRelationMapper.batch_save(list);
                }).compose(result -> {
                    return roleUserRelationMapper.list(F.field(RoleUserRelation::getRoleId).eq(F.params(RoleUserRelation::getRoleId)),
                            Map.of("roleId", roleId)).compose(ok -> {
                        return Future.succeededFuture(ok.stream().flatMap(ru -> Stream.of("permissions:" + ru.getUserId(), "roles:" + ru.getUserId())).toList());
                    });

                }).compose(permissionKeys -> {
                    return Future.fromCompletionStage(this.cacheStore.deleteAll(permissionKeys));
                }).onSuccess(ok -> {
                    context.end(Result.success(Boolean.TRUE).toBuffer());
                }).onFailure(context::fail);
    }

    @Override
    public void users(RoutingContext context) {
        String roleId = context.pathParam("roleId");
        SelectConditionStep<Record1<UUID>> where = roleUserRelationMapper.getDslContext()
                .select(F.field(RoleUserRelation::getUserId))
                .from(roleUserRelationMapper.getTable())
                .where(F.field(RoleUserRelation::getRoleId).eq(F.params(RoleUserRelation::getRoleId)));
        UserQueryVO query = Query.format(UserQueryVO.class, context);
        if (query.getPageSize() != null && query.getCurrentPage() != null) {
            userMapper.page(F.field(User::getId).in(where),
                            query.getCurrentPage(), query.getPageSize(),
                            Map.of("roleId", roleId))
                    .onSuccess(userPage -> {
                        List<UserDTO> list = userPage.getRecords().stream().map(UserDTO::new).toList();
                        Page<UserDTO> result = new Page<>();
                        result.setRecords(list);
                        result.setSize(userPage.getSize());
                        result.setCurrent(userPage.getCurrent());
                        result.setTotal(userPage.getTotal());
                        context.end(Result.success(result).toBuffer());
                    }).onFailure(context::fail);
        } else {
            userMapper.list(F.field(User::getId).in(where),
                            Map.of("roleId", roleId))
                    .onSuccess(users -> {
                        context.end(Result.success(users.stream().map(UserDTO::new).toList()).toBuffer());
                    }).onFailure(context::fail);
        }


    }

    @Override
    public void removeUser(RoutingContext context) {
        String roleId = context.pathParam("roleId");
        RemoveUserVO pojo = context.body().asPojo(RemoveUserVO.class);
        roleUserRelationMapper.delete(F.field(RoleUserRelation::getUserId).in(F.params(RoleUserRelation::getUserId))
                                .and(F.field(RoleUserRelation::getRoleId).eq(F.params(RoleUserRelation::getRoleId))),
                        Map.of("roleId", roleId, "userId", pojo.getUserIds()), List.of("userId"))
                .onSuccess(r -> {
                    context.end(Result.success(Boolean.TRUE).toBuffer());
                }).onFailure(context::fail);
    }

    private List<PermissionDTO> getPermissionList(Locale locale, String roleId, Boolean selected) {
        PermissionConstants.Role role = PermissionConstants.Role.valueOf(roleId);
        PermissionConstants[] values = PermissionConstants.values();
        return Arrays.stream(values)
                .filter(permissionConstants -> permissionConstants.getRoles().contains(role))
                .map(permissionConstants -> getPermissionDTO(locale, permissionConstants, selected))
                .toList();
    }

    @NotNull
    private static PermissionDTO getPermissionDTO(Locale locale, PermissionConstants permissionConstants, Boolean editable) {
        PermissionConstants.Permission permission = permissionConstants.getPermission();
        String group = permission.getGroup().name();
        String subGroup = permission.getSubGroup().name();
        String groupLabel = I18n.get("group." + group, locale);
        String subGroupLabel = I18n.get("group." + subGroup, locale);
        String permissionLabel = I18n.get("operate." + permission.getOperate().name(), locale);
        return new PermissionDTO(groupLabel, subGroupLabel, permissionLabel, group, subGroup, permission.toString(), editable);
    }
}
