package com.run.common.config;

import com.run.common.constants.DatabaseType;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/26  16:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class AppConfig {
    private DataBase database;

    private System system;

    public static AppConfig getDefault() {
        DataBase dataBase = new DataBase();
        dataBase.setType(DatabaseType.SQLITE);
        AppConfig appConfig = new AppConfig();
        System s = new System();
        s.setDataPath("data");
        appConfig.setDatabase(dataBase);
        appConfig.setSystem(s);
        return appConfig;
    }
}
