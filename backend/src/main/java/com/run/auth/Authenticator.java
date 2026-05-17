package com.run.auth;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.exception.ApiException;
import com.run.common.exception.ForbiddenException;
import com.run.dao.entity.Role;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
public class Authenticator implements Handler<RoutingContext> {
    private final List<Function<RoutingContext, PermissionConstants.Permission>> permissionCalls;
    private final List<PermissionConstants> permissionConstants;
    private final List<PermissionConstants.Role> roles;
    private final List<AggregatePermission> aggregatePermissions;
    private final PermissionConstants.Compare compare;

    /**
     * 通用权限检查方法
     *
     * @param permissions 权限集合
     * @param checker     权限检查函数
     * @param context     路由上下文
     * @return true表示已处理(成功或失败), false表示继续执行
     */
    private <T> boolean checkPermissionCollection(Collection<T> permissions,
                                                  PermissionChecker<T> checker,
                                                  RoutingContext context,
                                                  List<String> userRoles,
                                                  Map<String, Long> userPermissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            return false;
        }

        for (T permission : permissions) {
            boolean hasPerm = checker.check(permission, context, userRoles, userPermissions);

            if (hasPerm) {
                if (compare == PermissionConstants.Compare.OR) {
                    context.next();
                    return true;
                }
            } else {
                if (compare == PermissionConstants.Compare.AND) {
                    context.fail(new ForbiddenException("权限不足"));
                    return true;
                }
            }
        }
        return false;
    }


    @FunctionalInterface
    public interface PermissionChecker<T> {
        boolean check(T permission, RoutingContext context, List<String> userRoles, Map<String, Long> userPermissions);
    }

    @Override
    public void handle(RoutingContext context) {
        // 当前用户拥有的角色
        UserProfile user = context.user().get("user");
        List<String> roleIds = user.getRoles().stream().map(Role::getId).toList();
        // 当前用户拥有的权限
        Map<String, Long> permissions = user.getPermissions();

        if (CollectionUtils.isEmpty(this.permissionCalls) &&
                CollectionUtils.isEmpty(this.permissionConstants) &&
                CollectionUtils.isEmpty(this.aggregatePermissions) &&
                CollectionUtils.isEmpty(this.roles)) {
            context.next();
            return;
        }
        if (checkPermissionCollection(this.permissionCalls, Authenticator::hasPermission, context, roleIds, permissions))
            return;
        if (checkPermissionCollection(this.permissionConstants, Authenticator::hasPermission, context, roleIds, permissions))
            return;
        if (checkPermissionCollection(this.aggregatePermissions, Authenticator::hasPermission, context, roleIds, permissions))
            return;
        if (checkPermissionCollection(this.roles, Authenticator::hasPermission, context, roleIds, permissions)) return;
        if (compare == PermissionConstants.Compare.AND) {
            context.next();
        } else {
            context.fail(new ApiException(403, "权限不足"));
        }
    }

    protected static boolean hasPermission(Function<RoutingContext, PermissionConstants.Permission> permission,
                                           RoutingContext context,
                                           List<String> userRoles,
                                           Map<String, Long> userPermissions) {
        PermissionConstants.Permission p = permission.apply(context);
        String key = StringUtils.isEmpty(p.getResourceId()) ? p.toString() : p.getResourcePermissionKey(p.getResourceId());
        return userPermissions.containsKey(key) && (userPermissions.get(key) & p.bit()) > 0;
    }

    protected static boolean hasPermission(PermissionConstants permission,
                                           RoutingContext context,
                                           List<String> userRoles,
                                           Map<String, Long> userPermissions) {
        PermissionConstants.Permission p = permission.getPermission();
        String key = StringUtils.isEmpty(p.getResourceId()) ? p.toString() : p.getResourcePermissionKey(p.getResourceId());
        return userPermissions.containsKey(key) && (userPermissions.get(key) & p.bit()) > 0;
    }

    protected static boolean hasPermission(PermissionConstants.Role permission,
                                           RoutingContext context,
                                           List<String> userRoles, Map<String, Long> userPermissions) {
        return userRoles.contains(permission.toString());
    }

    protected static boolean hasPermission(AggregatePermission permission,
                                           RoutingContext context,
                                           List<String> userRoles,
                                           Map<String, Long> userPermissions) {
        return permission.hasPermission(context, userRoles, userPermissions);
    }

    private Authenticator(Builder builder) {
        this.permissionCalls = builder.permissionCalls;
        this.permissionConstants = builder.permissionConstants;
        this.roles = builder.roles;
        this.aggregatePermissions = builder.aggregatePermissions;
        this.compare = builder.compare;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {
        private List<Function<RoutingContext, PermissionConstants.Permission>> permissionCalls;
        private List<PermissionConstants> permissionConstants;
        private List<PermissionConstants.Role> roles;
        private List<AggregatePermission> aggregatePermissions;
        private PermissionConstants.Compare compare;

        private Builder() {
            this.permissionCalls = new ArrayList<>();
            this.permissionConstants = new ArrayList<>();
            this.roles = new ArrayList<>();
            this.aggregatePermissions = new ArrayList<>();
            this.compare = PermissionConstants.Compare.OR;
        }

        public Builder permissionCalls(List<Function<RoutingContext, PermissionConstants.Permission>> permissionCalls) {
            this.permissionCalls = permissionCalls;
            return this;
        }

        public Builder permissionConstants(List<PermissionConstants> permissionConstants) {
            this.permissionConstants = permissionConstants;
            return this;
        }

        public Builder roles(List<PermissionConstants.Role> roles) {
            this.roles = roles;
            return this;
        }

        public Builder aggregatePermissions(List<AggregatePermission> aggregatePermissions) {
            this.aggregatePermissions = aggregatePermissions;
            return this;
        }

        public Builder compare(PermissionConstants.Compare compare) {
            this.compare = compare;
            return this;
        }

        // Overloaded addPermission methods
        public Builder addPermission(Function<RoutingContext, PermissionConstants.Permission> permissionCall) {
            if (this.permissionCalls == null) {
                this.permissionCalls = new ArrayList<>();
            }
            this.permissionCalls.add(permissionCall);
            return this;
        }

        public Builder addPermission(PermissionConstants permissionConstant) {
            if (this.permissionConstants == null) {
                this.permissionConstants = new ArrayList<>();
            }
            this.permissionConstants.add(permissionConstant);
            return this;
        }

        public Builder addPermission(AggregatePermission aggregatePermission) {
            if (this.aggregatePermissions == null) {
                this.aggregatePermissions = new ArrayList<>();
            }
            this.aggregatePermissions.add(aggregatePermission);
            return this;
        }

        // Separate addRole method for Role
        public Builder addRole(PermissionConstants.Role role) {
            if (this.roles == null) {
                this.roles = new ArrayList<>();
            }
            this.roles.add(role);
            return this;
        }

        public Authenticator build() {
            return new Authenticator(this);
        }
    }

}
