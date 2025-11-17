package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.run.common.validator.Group;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  23:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(schemaName = "public", name = "user")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements BaseEntity<User> {
    @Column(name = "id", primaryKey = true)
    private UUID id;
    @Column(name = "email")
    @NotBlank(message = "邮箱不能为空", groups = Group.Create.class)
    private String email;
    @Column(name = "phone")
    private String phone;
    @Column(name = "nick_name")
    @NotBlank(message = "昵称不能为空", groups = Group.Create.class)
    private String nickname;
    @Column(name = "username")
    @NotBlank(message = "用户名不能为空", groups = Group.Create.class)
    private String username;
    @Column(name = "icon")
    private String icon;
    @Column(name = "password")
    @NotBlank(message = "密码不能为空", groups = Group.Create.class)
    @Max(value = 20, message = "密码最大值")
    private String password;

    @Column(name = "role")
    private String role;

    @Column(name = "create_time")
    private LocalDateTime createTime;
    @Column(name = "update_time")
    private LocalDateTime updateTime;

}
