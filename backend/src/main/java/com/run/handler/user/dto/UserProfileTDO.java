package com.run.handler.user.dto;

import com.run.auth.dto.UserProfile;
import com.run.common.util.CommonUtils;
import com.run.common.util.PermissionHexUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/21  22:26}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Setter
@Getter
@NoArgsConstructor
public class UserProfileTDO extends UserDTO {
    private Map<String, String> permissions;
    private List<RoleTDO> roles;

    public UserProfileTDO(UserProfile userProfile) {
        this.setId(userProfile.getId());
        this.setIcon(userProfile.getIcon());
        this.setUsername(userProfile.getUsername());
        Map<String, String> permissions = PermissionHexUtils.toHexMap(userProfile.getPermissions());
        this.setPermissions(permissions);
        List<RoleTDO> roles = userProfile.getRoles().stream().map(item -> {
            RoleTDO roleTDO = new RoleTDO();
            CommonUtils.copyProperties(item, roleTDO);
            return roleTDO;
        }).collect(Collectors.toCollection(ArrayList::new));
        String role = userProfile.getRole();
        if ("ADMIN".equals(role)) {
            roles.add(new RoleTDO("ADMIN", "超级管理员", true));
        }
        this.setRoles(roles);
        this.setEmail(userProfile.getEmail());
        this.setPhone(userProfile.getPhone());
        this.setNickname(userProfile.getNickname());
        this.setCreateTime(userProfile.getCreateTime());
        this.setUpdateTime(userProfile.getUpdateTime());
    }
}
