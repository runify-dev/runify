package com.run.common.config;

import com.run.common.constants.DatabaseType;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/26  16:18}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class DataBase {
    private DatabaseType type;
    private String username;
    private String password;
    private String host;
    private Integer port;
    private String database;
}
