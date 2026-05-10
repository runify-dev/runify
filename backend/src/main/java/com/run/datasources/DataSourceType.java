package com.run.datasources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 数据源类型枚举
 */
public enum DataSourceType {
    SQL("SQL", "SQL 数据库", "com/run/datasources/icon/sql.svg"),
    CACHE("CACHE", "缓存", "com/run/datasources/icon/cache.svg");

    private final String code;
    private final String message;
    private final String icon;

    DataSourceType(String code, String message, String iconPath) {
        this.code = code;
        this.message = message;
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(iconPath)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + iconPath);
            }
            this.icon = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getIcon() {
        return icon;
    }
}
