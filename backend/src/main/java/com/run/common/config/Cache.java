package com.run.common.config;

import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/20  23:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class Cache {
    /**
     * local /redis
     */
    private CacheType type;
    /**
     * redis://xx.xxx.xxx.xxx:6379
     */
    private String connectionString;

    private String password;
}
