package com.run.sql.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ModelColumnResolver {

    private static final ConcurrentHashMap<Class<?>, List<ModelColumnMeta>> COLUMN_CACHE = new ConcurrentHashMap<>();

    private ModelColumnResolver() {
    }

    public static List<ModelColumnMeta> resolve(Class<?> modelClass) {
        Objects.requireNonNull(modelClass, "modelClass");
        return COLUMN_CACHE.computeIfAbsent(modelClass, ModelColumnResolver::resolveUncached);
    }

    public static int columnCacheSize() {
        return COLUMN_CACHE.size();
    }

    public static void clearCache() {
        COLUMN_CACHE.clear();
    }

    private static List<ModelColumnMeta> resolveUncached(Class<?> modelClass) {
        LinkedHashMap<String, ModelColumnMeta> columns = new LinkedHashMap<>();

        for (Class<?> type : hierarchy(modelClass)) {
            for (Field field : type.getDeclaredFields()) {
                if (shouldSkipField(field)) {
                    continue;
                }

                Annotation column = findColumnAnnotation(field.getAnnotations());
                if (column == null) {
                    continue;
                }

                String propertyName = field.getName();
                String columnName = readColumnName(column, propertyName);
                boolean primaryKey = readPrimaryKey(column);
                columns.put(propertyName, ModelColumnMeta.forField(propertyName, columnName, primaryKey, field));
            }
        }

        for (Class<?> type : hierarchy(modelClass)) {
            for (Method method : type.getDeclaredMethods()) {
                if (shouldSkipMethod(method)) {
                    continue;
                }

                Annotation column = findColumnAnnotation(method.getAnnotations());
                if (column == null) {
                    continue;
                }

                String propertyName = getterToProperty(method.getName());
                String columnName = readColumnName(column, propertyName);
                boolean primaryKey = readPrimaryKey(column);
                columns.put(propertyName, ModelColumnMeta.forGetter(propertyName, columnName, primaryKey, method));
            }
        }

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("模型没有任何 @Column 字段或 getter: " + modelClass.getName());
        }

        return Collections.unmodifiableList(new ArrayList<>(columns.values()));
    }

    private static List<Class<?>> hierarchy(Class<?> modelClass) {
        List<Class<?>> result = new ArrayList<>();
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            result.add(current);
            current = current.getSuperclass();
        }
        Collections.reverse(result);
        return result;
    }

    private static boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || field.isSynthetic();
    }

    private static boolean shouldSkipMethod(Method method) {
        int modifiers = method.getModifiers();
        return Modifier.isStatic(modifiers)
                || method.isSynthetic()
                || method.getParameterCount() != 0
                || method.getReturnType() == Void.TYPE;
    }

    private static Annotation findColumnAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if ("Column".equals(annotation.annotationType().getSimpleName())) {
                return annotation;
            }
        }
        return null;
    }

    private static String readColumnName(Annotation column, String defaultName) {
        String name = readString(column, "name", null);
        return hasText(name) ? name : defaultName;
    }

    private static boolean readPrimaryKey(Annotation column) {
        try {
            Method method = column.annotationType().getMethod("primaryKey");
            Object value = method.invoke(column);
            if (value instanceof Boolean b) {
                return b;
            }
            throw new IllegalArgumentException("@Column.primaryKey() 必须返回 boolean: " + column.annotationType().getName());
        } catch (NoSuchMethodException e) {
            return false;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("读取 @Column.primaryKey() 失败: " + column.annotationType().getName(), e);
        }
    }

    private static String readString(Annotation annotation, String methodName, String defaultValue) {
        try {
            Method method = annotation.annotationType().getMethod(methodName);
            Object value = method.invoke(annotation);
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof String s) {
                return s;
            }
            throw new IllegalArgumentException("@Column." + methodName + "() 必须返回 String: " + annotation.annotationType().getName());
        } catch (NoSuchMethodException e) {
            return defaultValue;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("读取 @Column." + methodName + "() 失败: " + annotation.annotationType().getName(), e);
        }
    }

    private static String getterToProperty(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }

        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }

        return methodName;
    }

    private static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (value.length() > 1
                && Character.isUpperCase(value.charAt(0))
                && Character.isUpperCase(value.charAt(1))) {
            return value;
        }

        return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
