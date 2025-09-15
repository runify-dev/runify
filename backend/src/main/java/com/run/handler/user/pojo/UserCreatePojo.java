package com.run.handler.user.pojo;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/9/15  23:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class UserCreatePojo {
    private String email;
    private String phone;
    private String icon;
    private String nickname;
    private String password;
    private String username;
}
