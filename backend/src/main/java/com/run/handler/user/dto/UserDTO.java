package com.run.handler.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/14  22:05}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Setter
@Getter
@NoArgsConstructor
public class UserDTO {
    private UUID id;
    private String email;
    private String phone;
    private String icon;
    private String role;
    private String nickname;
    private String username;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
