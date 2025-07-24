package com.run.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.run.common.constants.DatabaseType;
import org.flywaydb.core.Flyway;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  22:15}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class MigrationModule extends AbstractModule {
    @Inject
    @Named("runify.server.datasource.url")
    private String url;

    @Inject
    @Named("runify.server.datasource.database")
    private String database;
    @Inject
    @Named("runify.server.datasource.username")
    private String username;
    @Inject
    @Named("runify.server.datasource.password")
    private String password;
    @Inject
    @Named("runify.server.datasource.host")
    private String host;
    @Inject
    @Named("runify.server.datasource.db_type")
    private DatabaseType dbType;
    @Inject
    @Named("runify.server.datasource.port")
    private Integer port;

    private void migrationsPgsql() {
        // 配置 Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(String.format("jdbc:postgresql://%s:%d/%s", this.host, this.port, this.database), this.username, password)
                .locations("classpath:migrations/pgsql") // 迁移脚本位置
                .baselineOnMigrate(true) // 如果不存在元数据表则自动创建
                .load();

        // 执行迁移
        flyway.migrate();
    }

    private void migrationsSqlite() {
        Path path = Paths.get("data/database");
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // 配置 Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(this.url, null, null)
                .locations("classpath:migrations/sqlite") // 迁移脚本位置
                .baselineOnMigrate(true) // 如果不存在元数据表则自动创建
                .load();
        // 执行迁移
        flyway.migrate();
    }

    @Override
    protected void configure() {
        if (dbType == DatabaseType.POSTGRESQL) {
            migrationsPgsql();
        } else {
            migrationsSqlite();
        }
    }
}
