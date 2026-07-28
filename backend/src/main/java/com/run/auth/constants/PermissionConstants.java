package com.run.auth.constants;


import io.vertx.ext.web.RoutingContext;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

public enum PermissionConstants {
    /**
     * ---------------------------资源权限---------------------------------------------------------------------------
     */
    APPLICATION_READ(new Permission(Group.APPLICATION, Group.APPLICATION, Operate.READ, 0), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_EDIT(new Permission(Group.APPLICATION, Group.APPLICATION, Operate.EDIT, 1), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_DELETE(new Permission(Group.APPLICATION, Group.APPLICATION, Operate.DELETE, 2), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_CREATE(new Permission(Group.APPLICATION, Group.APPLICATION, Operate.CREATE, 3), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    APPLICATION_FOLDER_EDIT(new Permission(Group.APPLICATION, Group.FOLDER, Operate.EDIT, 4), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_FOLDER_DELETE(new Permission(Group.APPLICATION, Group.FOLDER, Operate.DELETE, 5), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_FOLDER_CREATE(new Permission(Group.APPLICATION, Group.FOLDER, Operate.CREATE, 6), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_OVERVIEW_READ(new Permission(Group.APPLICATION, Group.OVERVIEW, Operate.READ, 7), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_SETTING_READ(new Permission(Group.APPLICATION, Group.SETTING, Operate.READ, 8), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    APPLICATION_CONVERSATION_LOG_READ(new Permission(Group.APPLICATION, Group.CONVERSATION_LOG, Operate.READ, 9), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    NOTE_READ(new Permission(Group.NOTE, Group.NOTE, Operate.READ, 0), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    NOTE_EDIT(new Permission(Group.NOTE, Group.NOTE, Operate.EDIT, 1), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    NOTE_DELETE(new Permission(Group.NOTE, Group.NOTE, Operate.DELETE, 2), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    NOTE_CREATE(new Permission(Group.NOTE, Group.NOTE, Operate.CREATE, 3), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    NOTE_FOLDER_EDIT(new Permission(Group.NOTE, Group.FOLDER, Operate.EDIT, 4), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    NOTE_FOLDER_DELETE(new Permission(Group.NOTE, Group.FOLDER, Operate.DELETE, 5), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    NOTE_FOLDER_CREATE(new Permission(Group.NOTE, Group.FOLDER, Operate.CREATE, 6), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    MODEL_READ(new Permission(Group.MODEL, Group.MODEL, Operate.READ, 0), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_EDIT(new Permission(Group.MODEL, Group.MODEL, Operate.EDIT, 1), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_DELETE(new Permission(Group.MODEL, Group.MODEL, Operate.DELETE, 2), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_CREATE(new Permission(Group.MODEL, Group.MODEL, Operate.CREATE, 3), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_FOLDER_EDIT(new Permission(Group.MODEL, Group.FOLDER, Operate.EDIT, 4), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_FOLDER_DELETE(new Permission(Group.MODEL, Group.FOLDER, Operate.DELETE, 5), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    MODEL_FOLDER_CREATE(new Permission(Group.MODEL, Group.FOLDER, Operate.CREATE, 6), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),


    DATASOURCE_READ(new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.READ, 0), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    DATASOURCE_EDIT(new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.EDIT, 1), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    DATASOURCE_DELETE(new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.DELETE, 2), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    DATASOURCE_CREATE(new Permission(Group.DATASOURCE, Group.DATASOURCE, Operate.CREATE, 3), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    DATASOURCE_FOLDER_EDIT(new Permission(Group.DATASOURCE, Group.FOLDER, Operate.EDIT, 4), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    DATASOURCE_FOLDER_DELETE(new Permission(Group.DATASOURCE, Group.FOLDER, Operate.DELETE, 5), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    DATASOURCE_FOLDER_CREATE(new Permission(Group.DATASOURCE, Group.FOLDER, Operate.CREATE, 6), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    PROJECT_READ(new Permission(Group.PROJECT, Group.PROJECT, Operate.READ, 0), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_EDIT(new Permission(Group.PROJECT, Group.PROJECT, Operate.EDIT, 1), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_DELETE(new Permission(Group.PROJECT, Group.PROJECT, Operate.DELETE, 2), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_CREATE(new Permission(Group.PROJECT, Group.PROJECT, Operate.CREATE, 3), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_FOLDER_EDIT(new Permission(Group.PROJECT, Group.FOLDER, Operate.EDIT, 4), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_FOLDER_DELETE(new Permission(Group.PROJECT, Group.FOLDER, Operate.DELETE, 5), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    PROJECT_FOLDER_CREATE(new Permission(Group.PROJECT, Group.FOLDER, Operate.CREATE, 6), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    SKILL_READ(new Permission(Group.SKILL, Group.SKILL, Operate.READ, 0), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    SKILL_EDIT(new Permission(Group.SKILL, Group.SKILL, Operate.EDIT, 1), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    SKILL_DELETE(new Permission(Group.SKILL, Group.SKILL, Operate.DELETE, 2), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    SKILL_CREATE(new Permission(Group.SKILL, Group.SKILL, Operate.CREATE, 3), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    SKILL_FOLDER_EDIT(new Permission(Group.SKILL, Group.FOLDER, Operate.EDIT, 4), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    SKILL_FOLDER_DELETE(new Permission(Group.SKILL, Group.FOLDER, Operate.DELETE, 5), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    SKILL_FOLDER_CREATE(new Permission(Group.SKILL, Group.FOLDER, Operate.CREATE, 6), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    KNOWLEDGE_READ(new Permission(Group.KNOWLEDGE, Group.KNOWLEDGE, Operate.READ, 0), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    KNOWLEDGE_EDIT(new Permission(Group.KNOWLEDGE, Group.KNOWLEDGE, Operate.EDIT, 1), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    KNOWLEDGE_DELETE(new Permission(Group.KNOWLEDGE, Group.KNOWLEDGE, Operate.DELETE, 2), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    KNOWLEDGE_CREATE(new Permission(Group.KNOWLEDGE, Group.KNOWLEDGE, Operate.CREATE, 3), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    KNOWLEDGE_FOLDER_EDIT(new Permission(Group.KNOWLEDGE, Group.FOLDER, Operate.EDIT, 4), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    KNOWLEDGE_FOLDER_DELETE(new Permission(Group.KNOWLEDGE, Group.FOLDER, Operate.DELETE, 5), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    KNOWLEDGE_FOLDER_CREATE(new Permission(Group.KNOWLEDGE, Group.FOLDER, Operate.CREATE, 6), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    INTEGRATION_READ(new Permission(Group.INTEGRATION, Group.INTEGRATION, Operate.READ, 0), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    INTEGRATION_EDIT(new Permission(Group.INTEGRATION, Group.INTEGRATION, Operate.EDIT, 1), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    INTEGRATION_DELETE(new Permission(Group.INTEGRATION, Group.INTEGRATION, Operate.DELETE, 2), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    INTEGRATION_CREATE(new Permission(Group.INTEGRATION, Group.INTEGRATION, Operate.CREATE, 3), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    INTEGRATION_FOLDER_EDIT(new Permission(Group.INTEGRATION, Group.FOLDER, Operate.EDIT, 4), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    INTEGRATION_FOLDER_DELETE(new Permission(Group.INTEGRATION, Group.FOLDER, Operate.DELETE, 5), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    INTEGRATION_FOLDER_CREATE(new Permission(Group.INTEGRATION, Group.FOLDER, Operate.CREATE, 6), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    TOOL_READ(new Permission(Group.TOOL, Group.TOOL, Operate.READ, 0), List.of(ResourcePermissionGroup.VIEW, ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    TOOL_EDIT(new Permission(Group.TOOL, Group.TOOL, Operate.EDIT, 1), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    TOOL_DELETE(new Permission(Group.TOOL, Group.TOOL, Operate.DELETE, 2), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    TOOL_CREATE(new Permission(Group.TOOL, Group.TOOL, Operate.CREATE, 3), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    TOOL_FOLDER_EDIT(new Permission(Group.TOOL, Group.FOLDER, Operate.EDIT, 4), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    TOOL_FOLDER_DELETE(new Permission(Group.TOOL, Group.FOLDER, Operate.DELETE, 5), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),
    TOOL_FOLDER_CREATE(new Permission(Group.TOOL, Group.FOLDER, Operate.CREATE, 6), List.of(ResourcePermissionGroup.MANAGE), List.of(Role.ADMIN, Role.USER)),

    /**
     * ---------------------------系统权限---------------------------------------------------------------------------
     */

    USER_MANAGEMENT_READ(new Permission(Group.SYSTEM_SETTING, Group.USER_MANAGEMENT, Operate.READ, 0), List.of(), List.of(Role.ADMIN)),
    USER_MANAGEMENT_EDIT(new Permission(Group.SYSTEM_SETTING, Group.USER_MANAGEMENT, Operate.EDIT, 1), List.of(), List.of(Role.ADMIN)),
    USER_MANAGEMENT_DELETE(new Permission(Group.SYSTEM_SETTING, Group.USER_MANAGEMENT, Operate.DELETE, 2), List.of(), List.of(Role.ADMIN)),
    USER_MANAGEMENT_CREATE(new Permission(Group.SYSTEM_SETTING, Group.USER_MANAGEMENT, Operate.CREATE, 3), List.of(), List.of(Role.ADMIN)),
    USER_MANAGEMENT_AUTHORIZATION(new Permission(Group.SYSTEM_SETTING, Group.USER_MANAGEMENT, Operate.AUTHORIZATION, 4), List.of(), List.of(Role.ADMIN)),

    ROLE_MANAGEMENT_READ(new Permission(Group.SYSTEM_SETTING, Group.ROLE_MANAGEMENT, Operate.READ, 0), List.of(), List.of(Role.ADMIN)),
    ROLE_MANAGEMENT_EDIT(new Permission(Group.SYSTEM_SETTING, Group.ROLE_MANAGEMENT, Operate.EDIT, 1), List.of(), List.of(Role.ADMIN)),
    ROLE_MANAGEMENT_DELETE(new Permission(Group.SYSTEM_SETTING, Group.ROLE_MANAGEMENT, Operate.DELETE, 2), List.of(), List.of(Role.ADMIN)),
    ROLE_MANAGEMENT_CREATE(new Permission(Group.SYSTEM_SETTING, Group.ROLE_MANAGEMENT, Operate.CREATE, 3), List.of(), List.of(Role.ADMIN));


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
        DATASOURCE,
        SKILL,
        KNOWLEDGE,
        INTEGRATION,
        TOOL,
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
        FOLDER,
        /**
         * 系统设置
         */
        SYSTEM_SETTING;
    }


    /**
     * 对应操作按钮
     */
    public enum Operate {
        READ,
        EDIT,
        CREATE,
        DELETE,
        AUTHORIZATION;
    }

    /**
     * 权限组
     * 相当于角色 但是这个只针对资源
     */
    public enum ResourcePermissionGroup {
        NOT_AUTH,
        /**
         * 查看
         */
        VIEW,
        /**
         * 管理
         */
        MANAGE,
        /**
         * 角色
         */
        ROLE
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
        String resourceId;
        int bitIndex;

        public Permission(Group group, Group subGroup, Operate operate, int bitIndex) {
            this.group = group;
            this.subGroup = subGroup;
            this.operate = operate;
            this.bitIndex = bitIndex;
        }

        public Permission(Group group, Group subGroup, Operate operate, int bitIndex, String resourceId) {
            this.group = group;
            this.operate = operate;
            this.subGroup = subGroup;
            this.bitIndex = bitIndex;
            this.resourceId = resourceId;
        }

        public long bit() {
            return 1L << this.bitIndex;
        }

        @Override
        public String toString() {
            return this.group + ":" + (this.subGroup != this.group ? this.subGroup + ":" : "") + this.operate;
        }

        public String getResourcePermissionKey(String resourceId) {
            return this.group + ":" + resourceId;
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
        return c -> new Permission(this.permission.group, this.permission.subGroup, this.permission.operate, this.permission.bitIndex, c.pathParam("resourceId"));
    }

    public Function<RoutingContext, Permission> getFolderPermission() {
        return c -> new Permission(this.permission.group, this.permission.subGroup, this.permission.operate, this.permission.bitIndex, c.pathParam("folderId"));
    }
}
