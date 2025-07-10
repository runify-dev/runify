package com.run.guice;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.run.common.util.PropertiesUtils;

import java.util.Properties;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  22:42}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class PropertiesModule implements Module {
    private Properties bootstrapConfiguration;

    public PropertiesModule(Properties bootstrapConfiguration) {
        this.bootstrapConfiguration = bootstrapConfiguration;
    }

    public PropertiesModule(String configurationFilePath) {
        if (configurationFilePath != null) {
            this.bootstrapConfiguration = PropertiesUtils.loadProperties(configurationFilePath);
        }
    }


    @Override
    public void configure(Binder binder) {
        Names.bindProperties(binder, bootstrapConfiguration);
    }
}

