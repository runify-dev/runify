package com.run.handler.user.pojo;

import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  17:20}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

@Data
public class LoginPojo {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;

    public static Schema schema() {
        return new JsonSchema().required(List.of("username", "password"))
                .addProperty("username", new StringSchema())
                .addProperty("password", new StringSchema());
    }
}
