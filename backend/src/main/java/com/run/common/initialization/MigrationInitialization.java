package com.run.common.initialization;

import com.run.common.config.AppConfig;
import com.run.common.config.DataBase;
import com.run.common.config.System;
import com.run.sql.dialect.SQLDialect;
import org.flywaydb.core.Flyway;


import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/27  20:58}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class MigrationInitialization {
    private final AppConfig appConfig;

    @Inject
    public MigrationInitialization(AppConfig appConfig) {
        this.appConfig = appConfig;

    }

    public void init() {
        DataBase database = appConfig.getDatabase();
        System system = appConfig.getSystem();
        if (database.getType() == SQLDialect.POSTGRESQL) {
            migrationsPgsql(database);
        } else if (database.getType() == SQLDialect.H2) {
            migrationsH2(system.getDataPath());
        } else {
            migrationsSqlite(system.getDataPath());
        }
    }

    private void migrationsPgsql(DataBase database) {
        // 配置 Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(String.format("jdbc:postgresql://%s:%d/%s", database.getHost(), database.getPort(), database.getDatabase()), database.getUsername(), database.getPassword())
                .locations("classpath:migrations/pgsql") // 迁移脚本位置
                .baselineOnMigrate(true) // 如果不存在元数据表则自动创建
                .load();

        // 执行迁移
        flyway.migrate();
    }

    private void migrationsH2(String dataPath) {
        Path path = Paths.get(dataPath + "/database/h2/");
        path = path.toAbsolutePath();
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String url = "jdbc:h2:file:" + path + "/runify;DATABASE_TO_UPPER=false";
        // 配置 Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(url, null, null)
                .locations("classpath:migrations/h2") // 迁移脚本位置
                .baselineOnMigrate(true) // 如果不存在元数据表则自动创建
                .load();
        // 执行迁移
        flyway.migrate();
    }

    private void migrationsSqlite(String dataPath) {
        Path path = Paths.get(dataPath + "/database/sqlite");
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String url = "jdbc:sqlite:" + path + "/runify.db";
        // 配置 Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(url, null, null)
                .locations("classpath:migrations/sqlite") // 迁移脚本位置
                .baselineOnMigrate(true) // 如果不存在元数据表则自动创建
                .load();
        // 执行迁移
        flyway.migrate();
    }


}
