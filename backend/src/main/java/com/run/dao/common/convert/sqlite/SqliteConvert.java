package com.run.dao.common.convert.sqlite;

import com.run.dao.common.convert.AbstractEntityConverter;
import com.run.dao.common.convert.Converter;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/14  00:03}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class SqliteConvert<T> extends AbstractEntityConverter<T> {

    public SqliteConvert(Class<T> clazz, Map<String, Converter<?, ?>> customize) {
        super(clazz, customize);
    }

    public SqliteConvert(Class<T> clazz) {
        super(clazz);
    }

    @Override
    public String getDefaultPackageName() {
        return "com.run.dao.common.convert.sqlite.impl";
    }
}
