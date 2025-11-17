package com.run.dao.common.convert.postgres;

import com.run.dao.common.convert.AbstractEntityConverter;
import com.run.dao.common.convert.Converter;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  22:11}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class PostgresConvert<T> extends AbstractEntityConverter<T> {

    public PostgresConvert(Class<T> clazz, Map<String, Converter<?, ?>> customize) {
        super(clazz, customize);
    }

    public PostgresConvert(Class<T> clazz) {
        super(clazz);
    }

    @Override
    public String getDefaultPackageName() {
        return "com.run.dao.common.convert.postgres.impl";
    }
}