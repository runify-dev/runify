package com.run.common.util;

import com.run.auth.constants.PermissionConstants;
import com.run.dao.entity.Application;
import com.run.dao.entity.ApplicationPermission;
import com.run.dao.entity.Role;
import com.run.dao.entity.RoleUserRelation;
import com.run.dao.mapper.ApplicationPermissionMapper;
import com.run.dao.mapper.ApplicationRelationMapper;
import com.run.dao.mapper.RoleMapper;
import com.run.dao.mapper.RoleUserRelationMapper;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.QueryResourcePojo;
import com.run.handler.conversation.vo.ApplicationQueryVO;
import com.run.sql.condition.Condition;
import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.run.sql.DSL.field;


public class ConditionCommonUtil {

    public static Future<Boolean> isApplicationRead(ApplicationPermissionMapper applicationPermissionMapper,
                                                    RoleMapper roleMapper,
                                                    RoleUserRelationMapper roleUserRelationMapper,
                                                    String userId) {
        PermissionConstants.Permission permission = PermissionConstants.APPLICATION_READ.getPermission();
        Condition permissionCondition = field(ApplicationPermission::getUserId).eq(userId)
                .and(field(ApplicationPermission::getPermission).eq(permission.toString()));
        Future<Boolean> hasPermission = applicationPermissionMapper.search(permissionCondition, Map.of())
                .map(rowSet -> rowSet.size() > 0);

        // 查询 ADMIN 和 USER 角色的 ID
        Condition roleCondition = field(Role::getType).eq(PermissionConstants.Role.ADMIN)
                .or(field(Role::getType).eq(PermissionConstants.Role.USER));
        Future<Boolean> hasRole = roleMapper.search(roleCondition, Map.of())
                .compose(roles -> {
                    if (roles.size() == 0) {
                        return Future.succeededFuture(false);
                    }
                    List<String> roleIds = new ArrayList<>();
                    for (Role role : roles) {
                        roleIds.add(role.getId());
                    }
                    // 查询用户是否有这些角色
                    Condition userRoleCondition = field(RoleUserRelation::getUserId).eq(userId)
                            .and(field(RoleUserRelation::getRoleId).in(roleIds));
                    return roleUserRelationMapper.search(userRoleCondition, Map.of())
                            .map(rowSet -> rowSet.size() > 0);
                });

        return Future.all(hasPermission, hasRole)
                .map(composite -> {
                    Boolean perm = composite.resultAt(0);
                    Boolean role = composite.resultAt(1);
                    return perm || role;
                });
    }
}
