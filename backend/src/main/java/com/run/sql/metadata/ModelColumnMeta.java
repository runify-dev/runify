package com.run.sql.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public final class ModelColumnMeta {
    private final String propertyName;
    private final String columnName;
    private final boolean primaryKey;
    private final Field field;
    private final Method getter;

    private ModelColumnMeta(String propertyName, String columnName, boolean primaryKey, Field field, Method getter) {
        this.propertyName = Objects.requireNonNull(propertyName, "propertyName");
        this.columnName = Objects.requireNonNull(columnName, "columnName");
        this.primaryKey = primaryKey;
        this.field = field;
        this.getter = getter;
    }

    public static ModelColumnMeta forField(String propertyName, String columnName, boolean primaryKey, Field field) {
        Objects.requireNonNull(field, "field");
        field.setAccessible(true);
        return new ModelColumnMeta(propertyName, columnName, primaryKey, field, null);
    }

    public static ModelColumnMeta forGetter(String propertyName, String columnName, boolean primaryKey, Method getter) {
        Objects.requireNonNull(getter, "getter");
        getter.setAccessible(true);
        return new ModelColumnMeta(propertyName, columnName, primaryKey, null, getter);
    }

    public String propertyName() {
        return propertyName;
    }

    public String columnName() {
        return columnName;
    }

    public boolean primaryKey() {
        return primaryKey;
    }

    public Object read(Object model) {
        Objects.requireNonNull(model, "model");
        try {
            if (getter != null) {
                return getter.invoke(model);
            }
            return field.get(model);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("读取模型字段失败: " + model.getClass().getName() + "." + propertyName, e);
        }
    }
}
