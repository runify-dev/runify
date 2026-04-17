package com.run.auth.constants;


import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Function;

public enum PermissionConstants {
    /**
     * ---------------------------资源权限---------------------------------------------------------------------------
     */
    APPLICATION_READ(new Permission(Group.APPLICATION, Group.APPLICATION, Operate.READ), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_EDIT(new Permission(Group.APPLICATION, Group.APPLICATION, Operate.EDIT), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_DELETE(new Permission(Group.APPLICATION, Group.APPLICATION, Operate.DELETE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_CREATE(new Permission(Group.APPLICATION, Group.APPLICATION, Operate.CREATE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    APPLICATION_FOLDER_EDIT(new Permission(Group.APPLICATION, Group.FOLDER, Operate.EDIT), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_FOLDER_DELETE(new Permission(Group.APPLICATION, Group.FOLDER, Operate.DELETE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_FOLDER_CREATE(new Permission(Group.APPLICATION, Group.FOLDER, Operate.CREATE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    APPLICATION_OVERVIEW_READ(new Permission(Group.APPLICATION, Group.OVERVIEW, Operate.READ), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    APPLICATION_CONVERSATION_LOG_READ(new Permission(Group.APPLICATION, Group.CONVERSATION_LOG, Operate.READ), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    NOTE_READ(new Permission(Group.NOTE, Group.NOTE, Operate.READ), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    NOTE_EDIT(new Permission(Group.NOTE, Group.NOTE, Operate.EDIT), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    NOTE_DELETE(new Permission(Group.NOTE, Group.NOTE, Operate.DELETE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    NOTE_CREATE(new Permission(Group.NOTE, Group.NOTE, Operate.CREATE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_READ(new Permission(Group.MODEL, Group.MODEL, Operate.READ), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_EDIT(new Permission(Group.MODEL, Group.MODEL, Operate.EDIT), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_DELETE(new Permission(Group.MODEL, Group.MODEL, Operate.DELETE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_CREATE(new Permission(Group.MODEL, Group.MODEL, Operate.CREATE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_READ(new Permission(Group.PROJECT, Group.PROJECT, Operate.READ), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_EDIT(new Permission(Group.PROJECT, Group.PROJECT, Operate.EDIT), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_DELETE(new Permission(Group.PROJECT, Group.PROJECT, Operate.DELETE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_CREATE(new Permission(Group.PROJECT, Group.PROJECT, Operate.CREATE), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    /**
     * ---------------------------系统权限---------------------------------------------------------------------------
     */
    USER_MANAGEMENT_READ(new Permission(Group.USER_MANAGEMENT, Group.USER_MANAGEMENT, Operate.READ), List.of(), List.of(Role.ADMIN)),
    USER_MANAGEMENT_EDIT(new Permission(Group.USER_MANAGEMENT, Group.USER_MANAGEMENT, Operate.EDIT), List.of(), List.of(Role.ADMIN)),
    USER_MANAGEMENT_DELETE(new Permission(Group.USER_MANAGEMENT, Group.USER_MANAGEMENT, Operate.DELETE), List.of(), List.of(Role.ADMIN)),
    USER_MANAGEMENT_CREATE(new Permission(Group.USER_MANAGEMENT, Group.USER_MANAGEMENT, Operate.CREATE), List.of(), List.of(Role.ADMIN)),
    USER_MANAGEMENT_AUTHORIZATION(new Permission(Group.USER_MANAGEMENT, Group.USER_MANAGEMENT, Operate.AUTHORIZATION), List.of(), List.of(Role.ADMIN)),

    ROLE_MANAGEMENT_READ(new Permission(Group.ROLE_MANAGEMENT, Group.ROLE_MANAGEMENT, Operate.READ), List.of(), List.of(Role.ADMIN)),
    ROLE_MANAGEMENT_EDIT(new Permission(Group.USER_MANAGEMENT, Group.ROLE_MANAGEMENT, Operate.EDIT), List.of(), List.of(Role.ADMIN)),
    ROLE_MANAGEMENT_DELETE(new Permission(Group.USER_MANAGEMENT, Group.ROLE_MANAGEMENT, Operate.DELETE), List.of(), List.of(Role.ADMIN)),
    ROLE_MANAGEMENT_CREATE(new Permission(Group.USER_MANAGEMENT, Group.ROLE_MANAGEMENT, Operate.CREATE), List.of(), List.of(Role.ADMIN));


    /**
     * 对应菜单
     */
    public enum Group {
        /**
         * 应用
         */
        APPLICATION,
        NOTE,
        PROJECT,
        MODEL,
        USER_MANAGEMENT,
        ROLE_MANAGEMENT,
        /*--------下面是子菜单 -------------------- */
        /**
         * 概览
         */
        OVERVIEW,
        /**
         * 设置
         */
        SETTING,
        /**
         * 对话日志
         */
        CONVERSATION_LOG,
        /**
         * 文件夹
         */
        FOLDER;
    }


    /**
     * 对应操作按钮
     */
    public enum Operate {
        READ,
        EDIT,
        CREATE,
        DELETE,
        AUTHORIZATION
    }

    /**
     * 权限组
     * 相当于角色 但是这个只针对资源
     */
    public enum ResourcePermissionGroup {
        /**
         * 查看
         */
        VIEW,
        /**
         * 管理
         */
        MANAGE
    }

    /**
     * 角色
     */
    public enum Role {
        ADMIN,
        USER
    }

    public enum Compare {
        OR,
        AND
    }

    @Getter
    public static class Permission {
        Group group;
        Group subGroup;
        Operate operate;
        String resourcePath;

        public Permission(Group group, Group subGroup, Operate operate) {
            this.group = group;
            this.subGroup = subGroup;
            this.operate = operate;
        }

        public Permission(Group group, Group subGroup, Operate operate, String resourcePath) {
            this.group = group;
            this.operate = operate;
            this.subGroup = subGroup;
            this.resourcePath = resourcePath;
        }

        public String toString(String resourcePath) {
            return this.group + ":" + (this.subGroup != this.group ? this.subGroup + ":" : "") + this.operate + ":" + resourcePath;
        }

        @Override
        public String toString() {
            return this.group + ":" + (this.subGroup != this.group ? this.subGroup + ":" : "") + this.operate + (StringUtils.isNotEmpty(this.resourcePath) ? ":" + this.resourcePath : "");
        }

    }

    PermissionConstants(Permission permission, List<ResourcePermissionGroup> resourcePermissionGroups, List<Role> roles) {
        this.permission = permission;
        this.resourcePermissionGroups = resourcePermissionGroups;
        this.roles = roles;
    }

    /**
     * 权限
     */
    @Getter
    Permission permission;
    /**
     * 资源权限组
     */
    @Getter
    List<ResourcePermissionGroup> resourcePermissionGroups;
    /**
     * 角色组
     */
    @Getter
    List<Role> roles;


    @Override
    public String toString() {
        return super.toString();
    }

    public Function<RoutingContext, Permission> getResourcePermission() {
        return c -> new Permission(this.permission.group, this.permission.subGroup, this.permission.operate, c.pathParam("resourceId"));
    }
}
