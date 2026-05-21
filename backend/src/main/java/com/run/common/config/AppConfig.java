package com.run.common.config;

import com.run.sql.dialect.SQLDialect;
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
    private String secretKey;
    private System system;
    private Cache cache;
    private Search search;

    public static AppConfig getDefault() {
        DataBase dataBase = new DataBase();
        dataBase.setType(SQLDialect.SQLITE);
        AppConfig appConfig = new AppConfig();
        System s = new System();
        s.setDataPath("data");
        appConfig.setDatabase(dataBase);
        appConfig.setSystem(s);
        Cache c = new Cache();
        c.setType(CacheType.LOCAL);
        Search search = new Search();
        search.setType(SearchType.LUCENE);
        return appConfig;
    }
}
