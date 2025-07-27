package com.run.dagger.module;

import com.run.common.config.AppConfig;
import com.run.common.constants.DatabaseType;
import dagger.Module;
import dagger.Provides;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Singleton;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  22:42}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class ConfigModule {
    private AppConfig appConfig;

    public ConfigModule(String configurationFilePath) {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(Paths.get(configurationFilePath))) {
            this.appConfig = yaml.loadAs(in, AppConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Provides
    @Singleton
    public AppConfig appConfig() {
        return appConfig;
    }

    @Provides
    @Singleton
    public DatabaseType dbType() {
        return appConfig.getDatabase().getType();
    }
}

