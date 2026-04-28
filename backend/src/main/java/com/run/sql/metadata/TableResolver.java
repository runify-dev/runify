package com.run.sql.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class TableResolver {

    private static final ConcurrentHashMap<Class<?>, TableMeta> TABLE_CACHE = new ConcurrentHashMap<>();

    private TableResolver() {
    }

    public static TableMeta resolve(Class<?> modelClass) {
        Objects.requireNonNull(modelClass, "modelClass");
        return TABLE_CACHE.computeIfAbsent(modelClass, TableResolver::resolveUncached);
    }

    public static int tableCacheSize() {
        return TABLE_CACHE.size();
    }

    public static void clearCache() {
        TABLE_CACHE.clear();
    }

    private static TableMeta resolveUncached(Class<?> modelClass) {
        Annotation table = findTableAnnotation(modelClass);
        if (table == null) {
            return new TableMeta(null, null, decapitalize(modelClass.getSimpleName()));
        }

        String name = readString(table, "name");
        if (!hasText(name)) {
            throw new IllegalArgumentException("@Table.name() 不能为空: " + modelClass.getName());
        }

        String catalogName = readString(table, "catalogName");
        String schemaName = readString(table, "schemaName");
        return new TableMeta(blankToNull(catalogName), blankToNull(schemaName), name);
    }

    /**
     * 不强绑定某一个 @Table 包名。
     * 只要注解类名叫 Table，并且有 name()/catalogName()/schemaName() 方法，就可以识别。
     */
    private static Annotation findTableAnnotation(Class<?> modelClass) {
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            for (Annotation annotation : current.getAnnotations()) {
                if ("Table".equals(annotation.annotationType().getSimpleName())) {
                    return annotation;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static String readString(Annotation annotation, String methodName) {
        try {
            Method method = annotation.annotationType().getMethod(methodName);
            Object value = method.invoke(annotation);
            if (value == null) {
                return null;
            }
            if (value instanceof String s) {
                return s;
            }
            throw new IllegalArgumentException("@Table." + methodName + "() 必须返回 String: " + annotation.annotationType().getName());
        } catch (NoSuchMethodException e) {
            if ("catalogName".equals(methodName) || "schemaName".equals(methodName)) {
                return null;
            }
            throw new IllegalArgumentException("@Table 注解必须提供 " + methodName + "() 方法: " + annotation.annotationType().getName(), e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("读取 @Table." + methodName + "() 失败: " + annotation.annotationType().getName(), e);
        }
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
}
