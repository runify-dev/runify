package com.run.dagger.module;

import com.run.common.config.AppConfig;
import com.run.common.config.DataBase;

import com.run.sql.dialect.SQLDialect;
import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import org.yaml.snakeyaml.Yaml;

import javax.inject.Singleton;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  22:42}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class ConfigModule {
    private AppConfig appConfig;

    interface InitConfig {
        boolean support();

        AppConfig get();
    }

    static class YamlInitConfig implements InitConfig {
        private final String configurationFilePath = System.getProperty(
                "runify.config",
                "/opt/runify/conf/runify.yaml"
        );

        @Override
        public boolean support() {
            String runifyConfig = System.getenv("RUNIFY_CONFIG");
            return (Strings.CS.equals("YAML", runifyConfig) || StringUtils.isEmpty(runifyConfig)) && Files.exists(Paths.get(configurationFilePath));
        }

        @Override
        public AppConfig get() {
            Yaml yaml = new Yaml();
            try (InputStream in = Files.newInputStream(Paths.get(configurationFilePath))) {
                return yaml.loadAs(in, AppConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return AppConfig.getDefault();
        }
    }

    static class EnvInitConfig implements InitConfig {

        @Override
        public boolean support() {
            String config = System.getenv("RUNIFY_CONFIG");
            return Strings.CS.equals("ENV", config) || StringUtils.isEmpty(config);
        }

        @Override
        public AppConfig get() {
            DataBase dataBase = new DataBase();
            String systemDataPath = System.getenv("RUNIFY_SYSTEM_DATA_PATH");
            String databaseType = System.getenv("RUNIFY_DATABASE_TYPE");
            com.run.common.config.System system = new com.run.common.config.System();
            system.setDataPath(Optional.ofNullable(systemDataPath).orElse("data"));
            dataBase.setType(Optional.ofNullable(databaseType).map(SQLDialect::valueOf).orElse(SQLDialect.SQLITE));
            dataBase.setHost(Optional.ofNullable(System.getenv("RUNIFY_DATABASE_HOST")).orElse("127.0.0.1"));
            dataBase.setPort(Optional.ofNullable(System.getenv("RUNIFY_DATABASE_PORT")).map(Integer::valueOf).orElse(5432));
            dataBase.setUsername(Optional.ofNullable(System.getenv("RUNIFY_DATABASE_USERNAME")).orElse("postgres"));
            dataBase.setPassword(Optional.ofNullable(System.getenv("RUNIFY_DATABASE_PASSWORD")).orElse("postgres"));
            dataBase.setDatabase(Optional.ofNullable(System.getenv("RUNIFY_DATABASE_DATABASE")).orElse("runify"));
            AppConfig result = new AppConfig();
            result.setDatabase(dataBase);
            result.setSystem(system);
            return result;
        }
    }

    public ConfigModule() {
        List<InitConfig> initConfigs = List.of(new YamlInitConfig(), new EnvInitConfig());
        for (InitConfig initConfig : initConfigs) {
            if (initConfig.support()) {
                this.appConfig = initConfig.get();
                return;
            }
        }
    }

    @Provides
    @Singleton
    public AppConfig appConfig() {
        return appConfig;
    }

    @Provides
    @Singleton
    public SQLDialect dbType() {
        return appConfig.getDatabase().getType();
    }
}

