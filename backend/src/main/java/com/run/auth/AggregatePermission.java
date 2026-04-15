package com.run.auth;

import com.run.auth.constants.PermissionConstants;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Getter
public class AggregatePermission {
    private List<Function<RoutingContext, PermissionConstants.Permission>> permissionCalls;
    private List<PermissionConstants> permissionConstants;
    private List<AggregatePermission> aggregatePermissions;
    private List<PermissionConstants.Role> roles;
    private PermissionConstants.Compare compare;

    private AggregatePermission(Builder builder) {
        this.permissionCalls = builder.permissionCalls;
        this.permissionConstants = builder.permissionConstants;
        this.aggregatePermissions = builder.aggregatePermissions;
        this.roles = builder.roles;
        this.compare = builder.compare;
    }

    public boolean hasPermission(RoutingContext context,
                                 List<String> userRoles,
                                 List<String> userPermissions) {
        if (CollectionUtils.isEmpty(this.permissionCalls) &&
                CollectionUtils.isEmpty(this.permissionConstants) &&
                CollectionUtils.isEmpty(this.aggregatePermissions) &&
                CollectionUtils.isEmpty(this.roles)) {
            return true;
        }
        for (Function<RoutingContext, PermissionConstants.Permission> permissionCall : permissionCalls) {
            boolean hasPerm = Authenticator.hasPermission(permissionCall, context, userRoles, userPermissions);
            if (hasPerm) {
                if (compare == PermissionConstants.Compare.OR) {
                    return false;
                }
            } else {
                if (compare == PermissionConstants.Compare.AND) {
                    return true;
                }
            }

        }
        for (PermissionConstants permission : permissionConstants) {
            boolean hasPerm = Authenticator.hasPermission(permission, context, userRoles, userPermissions);
            if (hasPerm) {
                if (compare == PermissionConstants.Compare.OR) {
                    return false;
                }
            } else {
                if (compare == PermissionConstants.Compare.AND) {
                    return true;
                }
            }
        }
        for (PermissionConstants.Role role : roles) {
            boolean hasPerm = Authenticator.hasPermission(role, context, userRoles, userPermissions);
            if (hasPerm) {
                if (compare == PermissionConstants.Compare.OR) {
                    return false;
                }
            } else {
                if (compare == PermissionConstants.Compare.AND) {
                    return true;
                }
            }
        }
        for (AggregatePermission aggregatePermission : aggregatePermissions) {
            boolean hasPerm = aggregatePermission.hasPermission(context, userRoles, userPermissions);
            if (hasPerm) {
                if (compare == PermissionConstants.Compare.OR) {
                    return false;
                }
            } else {
                if (compare == PermissionConstants.Compare.AND) {
                    return true;
                }
            }
        }
        return compare == PermissionConstants.Compare.AND;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Function<RoutingContext, PermissionConstants.Permission>> permissionCalls;
        private List<PermissionConstants> permissionConstants;
        private List<AggregatePermission> aggregatePermissions;
        private List<PermissionConstants.Role> roles;
        private PermissionConstants.Compare compare;

        private Builder() {
            this.permissionCalls = new ArrayList<>();
            this.permissionConstants = new ArrayList<>();
            this.aggregatePermissions = new ArrayList<>();
            this.roles = new ArrayList<>();
            this.compare = PermissionConstants.Compare.OR;
        }

        public Builder permissionCalls(List<Function<RoutingContext, PermissionConstants.Permission>> permissionCalls) {
            this.permissionCalls = permissionCalls;
            return this;
        }

        public Builder aggregatePermissions(List<AggregatePermission> aggregatePermissions) {
            this.aggregatePermissions = aggregatePermissions;
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

        public Builder compare(PermissionConstants.Compare compare) {
            this.compare = compare;
            return this;
        }

        // Overloaded addPermission methods for permissionCalls and permissionConstants
        public Builder addPermission(Function<RoutingContext, PermissionConstants.Permission> permissionCall) {
            if (this.permissionCalls == null) {
                this.permissionCalls = new ArrayList<>();
            }
            this.permissionCalls.add(permissionCall);
            return this;
        }

        public Builder addPermission(AggregatePermission aggregatePermission) {
            if (this.aggregatePermissions == null) {
                this.aggregatePermissions = new ArrayList<>();
            }
            this.aggregatePermissions.add(aggregatePermission);
            return this;
        }

        public Builder addPermission(PermissionConstants permissionConstant) {
            if (this.permissionConstants == null) {
                this.permissionConstants = new ArrayList<>();
            }
            this.permissionConstants.add(permissionConstant);
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

        public AggregatePermission build() {
            return new AggregatePermission(this);
        }
    }

}
