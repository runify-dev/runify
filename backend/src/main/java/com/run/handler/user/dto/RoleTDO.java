package com.run.handler.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/21  22:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleTDO {
    private String id;
    private String name;
    private Boolean internal;
}
