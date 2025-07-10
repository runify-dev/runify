package com.run.handler.user.pojo;

import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.vertx.sqlclient.Row;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/6  00:32}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class UserPojo {
    private UUID id;
    private String email;
    private String phone;
    private String icon;

    private String nickname;
    private String username;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public UserPojo() {

    }

    public UserPojo(Row row) {
        this.id = row.getUUID("id");
        this.email = row.getString("email");
        this.phone = row.getString("phone");
        this.nickname = row.getString("nick_name");
        this.icon = row.getString("icon");
        this.username = row.getString("username");
        this.createTime = row.getLocalDateTime("create_time");
        this.updateTime = row.getLocalDateTime("update_time");
    }

    public static Schema schema() {
        return new JsonSchema()
                .required(List.of("id", "email", "icon", "username", "createTime"))
                .addProperty("id", new StringSchema().description("用户id"))
                .addProperty("email", new StringSchema().description("邮箱"))
                .addProperty("phone", new StringSchema().description("手机号"))
                .addProperty("icon", new StringSchema().description("icon"))
                .addProperty("nickname", new StringSchema().description("昵称"))
                .addProperty("username", new StringSchema().description("用户名"))
                .addProperty("createTime", new StringSchema().description("创建时间"))
                .addProperty("updateTime", new StringSchema().description("修改时间"));
    }
}
