package com.run.auth.dto;

import com.run.dao.entity.Role;
import com.run.dao.entity.User;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/21  00:58}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class UserProfile extends User {
    private List<Role> roles;

    private Map<String, Long> permissions;
}
