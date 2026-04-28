package com.run.sql.lambda;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class LambdaColumnResolver {

    private static final ConcurrentHashMap<Class<?>, SerializedLambda> SERIALIZED_LAMBDA_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<LambdaKey, String> COLUMN_CACHE = new ConcurrentHashMap<>();

    private LambdaColumnResolver() {
    }

    public static <T> String resolve(SerializableFunction<T, ?> getter) {
        Objects.requireNonNull(getter, "getter");
        SerializedLambda lambda = serializedLambda(getter);

        LambdaKey key = new LambdaKey(
                lambda.getImplClass(),
                lambda.getImplMethodName(),
                lambda.getImplMethodSignature()
        );

        return COLUMN_CACHE.computeIfAbsent(key, ignored -> resolveColumnName(lambda));
    }

    public static int serializedLambdaCacheSize() {
        return SERIALIZED_LAMBDA_CACHE.size();
    }

    public static int columnCacheSize() {
        return COLUMN_CACHE.size();
    }

    public static void clearCache() {
        SERIALIZED_LAMBDA_CACHE.clear();
        COLUMN_CACHE.clear();
    }

    private static SerializedLambda serializedLambda(Serializable lambda) {
        return SERIALIZED_LAMBDA_CACHE.computeIfAbsent(lambda.getClass(), ignored -> {
            try {
                Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
                writeReplace.setAccessible(true);

                Object serialized = writeReplace.invoke(lambda);
                if (serialized instanceof SerializedLambda sl) {
                    return sl;
                }

                throw new IllegalArgumentException("无法解析 SerializedLambda: " + serialized);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("无法解析 lambda，请确认参数是可序列化的方法引用", e);
            }
        });
    }

    private static String resolveColumnName(SerializedLambda lambda) {
        String implClassName = lambda.getImplClass().replace('/', '.');
        String methodName = lambda.getImplMethodName();

        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("无法从 lambda 中解析方法名");
        }

        if (methodName.startsWith("lambda$")) {
            throw new IllegalArgumentException("field(...) 只支持方法引用，例如 Model::getName，不支持普通 lambda 表达式");
        }

        Class<?> modelClass = loadClass(implClassName);
        Method method = findNoArgMethod(modelClass, methodName);

        String methodColumnName = method == null ? null : readColumnName(method.getAnnotations());
        if (hasText(methodColumnName)) {
            return methodColumnName;
        }

        String propertyName = getterToProperty(methodName);
        Field field = findField(modelClass, propertyName);

        String fieldColumnName = field == null ? null : readColumnName(field.getAnnotations());
        if (hasText(fieldColumnName)) {
            return fieldColumnName;
        }

        return propertyName;
    }

    private static Class<?> loadClass(String className) {
        try {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            if (tccl != null) {
                return Class.forName(className, false, tccl);
            }
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("无法加载 lambda 所属类: " + className, e);
        }
    }

    private static Method findNoArgMethod(Class<?> type, String methodName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            current = current.getSuperclass();
        }

        for (Method method : type.getMethods()) {
            if (method.getParameterCount() == 0 && method.getName().equals(methodName)) {
                return method;
            }
        }

        return null;
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 不强绑定某一个 @Column 包名。
     * 只要注解类名叫 Column，并且有 name() 方法，就可以识别。
     */
    private static String readColumnName(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            if (!"Column".equals(annotationType.getSimpleName())) {
                continue;
            }

            try {
                Method nameMethod = annotationType.getMethod("name");
                Object value = nameMethod.invoke(annotation);

                if (value instanceof String name && hasText(name)) {
                    return name;
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("@Column 注解必须提供 name() 方法: " + annotationType.getName(), e);
            }
        }

        return null;
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

    private record LambdaKey(String implClass, String implMethodName, String implMethodSignature) {
    }
}
