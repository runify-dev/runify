package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.run.common.constants.DatabaseType;
import com.run.common.validator.Group;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.sqlclient.Row;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
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
public class User implements BaseEntity<User> {
    @Column(name = "id", primaryKey = true)
    private UUID id;
    @Column(name = "email")
    @NotBlank(message = "邮箱不能为空", groups = Group.Create.class)
    private String email;
    @Column(name = "phone")
    @NotBlank(message = "手机号不能为空", groups = Group.Create.class)
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
    @Column(name = "create_time")
    private LocalDateTime createTime;
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public User(Row row) {
        this.id = row.getUUID("id");
        this.email = row.getString("email");
        this.phone = row.getString("phone");
        this.nickname = row.getString("nick_name");
        this.username = row.getString("username");
        this.password = row.getString("password");
        this.icon = row.getString("icon");
        this.createTime = row.getLocalDateTime("create_time");
        this.updateTime = row.getLocalDateTime("update_time");
    }


    @Override
    @JsonIgnore
    public Map<DatabaseType, BaseConvert<User>> getConvertMap() {
        return Map.of(DatabaseType.SQLITE, new Sqlite(),
                DatabaseType.POSTGRESQL, new Pgsql(),
                DatabaseType.H2, new Pgsql());
    }


    class Pgsql implements BaseConvert<User> {
        @Override
        public User mapTo(Row row) {
            return new User(row);
        }
    }

    class Sqlite implements BaseConvert<User> {

        @Override
        public User mapTo(Row row) {
            return new User(row);
        }
    }
}
