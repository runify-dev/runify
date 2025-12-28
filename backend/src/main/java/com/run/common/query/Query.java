package com.run.common.query;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.pojo.FieldMapping;
import com.run.common.query.annotations.QueryParams;
import com.run.common.util.ClassScanUtil;
import com.run.dao.common.convert.annotations.For;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/22  22:36}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class Query {
    private static final ConcurrentMap<Class<?>, Children> cache = new ConcurrentHashMap<>();
    protected static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    protected static List<Class<? extends Deserialize>> classList = ClassScanUtil.getClassList("com.run.common.query.impl", Deserialize.class);

    public static <T> T format(Class<T> clazz, RoutingContext context) {
        Children<T> children = cache.computeIfAbsent(clazz, c -> new Children<>(c));
        return children.format(context);
    }

    private static class Children<T> {
        protected MethodHandle constructorHandle;
        protected Map<Class<?>, Deserialize<?>> deserializes;
        protected Map<String, Deserialize<?>> customizeDeserializes;
        protected List<FieldMapping> fieldMappings;

        public Children(Class<T> clazz) {
            this.constructorHandle = getCachedConstructor(clazz);
            this.deserializes = getDeserializes();
            this.customizeDeserializes = getCustomizeDeserializes(clazz);
            this.fieldMappings = getFieldMappings(clazz);
        }

        public Map<String, Deserialize<?>> getCustomizeDeserializes(Class<T> clazz) {
            try {
                MethodHandle handle = LOOKUP.findStaticGetter(clazz, "CUSTOMIZE_DESERIALIZE", Object.class);
                return (Map<String, Deserialize<?>>) handle.invoke();
            } catch (Throwable e) {
                return new HashMap<>();
            }
        }

        public Map<Class<?>, Deserialize<?>> getDeserializes() {
            return classList.stream()
                    .flatMap(clazz -> {
                        For annotation = clazz.getAnnotation(For.class);
                        try {
                            Deserialize<?> deserialize = clazz.getConstructor().newInstance();
                            return Arrays.stream(annotation.value())
                                    .map(targetClass -> new DefaultKeyValue<>(targetClass, deserialize));
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to instantiate converter: " + clazz.getName(), e);
                        }
                    })
                    .collect(Collectors.toMap(
                            DefaultKeyValue::getKey,
                            DefaultKeyValue::getValue,
                            (existing, replacement) -> existing
                    ));

        }

        private MethodHandle getCachedConstructor(Class<T> clazz) {
            try {
                return LOOKUP.findConstructor(clazz, MethodType.methodType(void.class));
            } catch (Exception e) {
                throw new RuntimeException("No accessible no-arg constructor for: " + clazz.getName(), e);
            }
        }

        private static List<FieldMapping> getFieldMappings(Class<?> clazz) {
            List<FieldMapping> mappings = new ArrayList<>();
            Field[] fields = FieldUtils.getAllFields(clazz);
            for (Field field : fields) {
                if (field.isAnnotationPresent(QueryParams.class)) {
                    try {
                        field.setAccessible(true);
                        String columnName = field.getAnnotation(QueryParams.class).name();
                        MethodHandle getter = LOOKUP.unreflectGetter(field);
                        MethodHandle setter = LOOKUP.unreflectSetter(field);
                        mappings.add(new FieldMapping(field, columnName, getter, setter));
                    } catch (IllegalAccessException e) {
                        // 忽略无法访问的字段
                    }
                }

            }
            return mappings;
        }

        @SneakyThrows
        public T format(RoutingContext context) {
            T instance = (T) constructorHandle.invoke();
            for (FieldMapping field : this.fieldMappings) {
                Class<?> type = field.field().getType();
                Deserialize<?> deserialize = customizeDeserializes.getOrDefault(field.field().getName(), deserializes.get(type.isEnum() || Enum.class.isAssignableFrom(type) ? Enum.class : type));
                QueryParams annotation = field.field().getAnnotation(QueryParams.class);
                Object value = deserialize.deserialize(context, annotation, type);
                field.setValue(instance, value);
            }
            return instance;
        }
    }


}
