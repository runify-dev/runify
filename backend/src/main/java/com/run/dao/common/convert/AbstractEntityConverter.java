package com.run.dao.common.convert;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.pojo.FieldMapping;
import com.run.common.util.ClassScanUtil;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.convert.annotations.For;
import io.vertx.sqlclient.Row;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  22:01}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public abstract class AbstractEntityConverter<T> implements EntityConvert<T> {
    protected final MethodHandle constructorHandle;
    protected Map<Class<?>, Converter<?, ?>> converters;
    protected final Map<String, Converter<?, ?>> customizeConverters;
    protected final List<FieldMapping> fieldMappings;
    protected static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();


    public AbstractEntityConverter(Class<T> clazz, Map<String, Converter<?, ?>> customize) {
        this.fieldMappings = getFieldMappings(clazz);
        this.constructorHandle = getCachedConstructor(clazz);
        this.customizeConverters = customize;
        converters = getConverters(getDefaultPackageName());
    }

    public AbstractEntityConverter(Class<T> clazz) {
        this.fieldMappings = getFieldMappings(clazz);
        this.constructorHandle = getCachedConstructor(clazz);
        this.customizeConverters = Map.of();
        converters = getConverters(getDefaultPackageName());
    }

    public abstract String getDefaultPackageName();

    public static Map<Class<?>, Converter<?, ?>> getConverters(String packageName) {
        List<Class<? extends Converter>> classList = ClassScanUtil.getClassList(packageName, Converter.class);
        return classList.stream()
                .flatMap(clazz -> {
                    For annotation = clazz.getAnnotation(For.class);
                    try {
                        Converter<?, ?> converter = clazz.getConstructor().newInstance();
                        return Arrays.stream(annotation.value())
                                .map(targetClass -> new DefaultKeyValue<>(targetClass, converter));
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


    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public T mapTo(Row row) {
        T instance = (T) constructorHandle.invoke();
        for (FieldMapping field : this.fieldMappings) {
            Class<?> type = field.field().getType();
            Converter<?, ?> converter = customizeConverters.getOrDefault(field.field().getName(), converters.get(type.isEnum() || Enum.class.isAssignableFrom(type) ? Enum.class : type));
            Object value = converter.deserialize(row, field.columnName(), type);
            field.setValue(instance, value);
        }
        return instance;
    }

    @Override
    public Map<String, Object> toMap(Object o) {
        HashMap<String, Object> result = new HashMap<>();
        for (FieldMapping field : this.fieldMappings) {
            Class<?> type = field.field().getType();
            Converter<?, ?> converter = (type.isEnum() || Enum.class.isAssignableFrom(type)) ? converters.get(Enum.class) : converters.get(type);
            Object value = converter.serialize(o, field.field().getName());
            result.put(field.columnName(), value);
        }
        return result;
    }

    private MethodHandle getCachedConstructor(Class<T> clazz) {
        try {
            return LOOKUP.findConstructor(clazz, MethodType.methodType(void.class));
        } catch (Exception e) {
            throw new RuntimeException("No accessible no-arg constructor for: " + clazz.getName(), e);
        }
    }


    private List<FieldMapping> getFieldMappings(Class<T> clazz) {
        List<FieldMapping> mappings = new ArrayList<>();
        Field[] fields = FieldUtils.getAllFields(clazz);
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                try {
                    field.setAccessible(true);
                    String columnName = field.getAnnotation(Column.class).name();
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
}
