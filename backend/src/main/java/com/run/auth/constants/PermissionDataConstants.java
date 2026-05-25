package com.run.auth.constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PermissionDataConstants {
    /**
     * 所有View权限的Map
     */
    public final static Map<PermissionConstants.Group, List<PermissionConstants>> viewPermission = Arrays.stream(PermissionConstants.values())
            .filter(p -> p.getResourcePermissionGroups().contains(PermissionConstants.ResourcePermissionGroup.VIEW))
            .collect(Collectors.groupingBy(p -> p.getPermission().getGroup()));
    /**
     * 所有Manage权限的Map
     */
    public final static Map<PermissionConstants.Group, List<PermissionConstants>> managePermission = Arrays.stream(PermissionConstants.values())
            .filter(p -> p.getResourcePermissionGroups().contains(PermissionConstants.ResourcePermissionGroup.MANAGE))
            .collect(Collectors.groupingBy(p -> p.getPermission().getGroup()));
    /**
     * 所有权限的BitIndex的Map
     */
    public final static Map<String, PermissionConstants.Permission> permissionMap = Arrays.stream(PermissionConstants.values())
            .collect(Collectors.toMap(p -> p.getPermission().toString(), PermissionConstants::getPermission));
    /**
     * view的权限集合
     */
    public final static Map<PermissionConstants.Group, Long> viewPermissionBit = Arrays.stream(PermissionConstants.values())
            .filter(p -> p.getResourcePermissionGroups().contains(PermissionConstants.ResourcePermissionGroup.VIEW))
            .collect(Collectors.groupingBy(p -> p.getPermission().getGroup(),
                    Collectors.mapping(p -> p.getPermission().bit(), Collectors.reducing(0L, (a, b) -> a | b))));
    /**
     * manage的权限集合
     */
    public final static Map<PermissionConstants.Group, Long> managePermissionBit = Arrays.stream(PermissionConstants.values())
            .filter(p -> p.getResourcePermissionGroups().contains(PermissionConstants.ResourcePermissionGroup.MANAGE))
            .collect(Collectors.groupingBy(p -> p.getPermission().getGroup(),
                    Collectors.mapping(p -> p.getPermission().bit(), Collectors.reducing(0L, (a, b) -> a | b))));

    public final static Map<PermissionConstants.ResourcePermissionGroup, Map<PermissionConstants.Group, Long>> resourcePermissionMap =
            Map.of(PermissionConstants.ResourcePermissionGroup.VIEW, viewPermissionBit,
                    PermissionConstants.ResourcePermissionGroup.MANAGE, managePermissionBit);

}
