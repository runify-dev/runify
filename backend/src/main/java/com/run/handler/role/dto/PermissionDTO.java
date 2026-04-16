package com.run.handler.role.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/16  21:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PermissionDTO {
    private String groupLabel;
    private String subGroupLabel;
    private String permissionGroupLabel;

    private String group;
    private String subGroup;
    private String permission;
    private Boolean selected;
}
