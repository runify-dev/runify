package com.run.sql.model;

import com.run.sql.Value;
import com.run.sql.lambda.LambdaColumnResolver;
import com.run.sql.lambda.SerializableFunction;
import com.run.sql.render.RenderContext;

import java.util.Objects;

public final class Param<T> implements Value<T> {
    private final String name;
    private final T value;
    private final boolean hasValue;

    private Param(String name, T value, boolean hasValue) {
        this.name = Objects.requireNonNull(name, "name");
        this.value = value;
        this.hasValue = hasValue;
    }

    public String name() {
        return name;
    }

    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    public boolean isNamedOnly() {
        return !hasValue;
    }

    public static <T> Param<T> of(String name, T value) {
        return new Param<>(name, value, true);
    }

    /**
     * 使用方法引用解析参数名。
     *
     * <pre>{@code
     * param(UserModel::getName, "张三") -> #{user_name}
     * }</pre>
     */
    public static <M, T> Param<T> of(SerializableFunction<M, T> getter, T value) {
        return new Param<>(LambdaColumnResolver.resolve(getter), value, true);
    }

    public static Param<Object> named(String name) {
        return new Param<>(name, null, false);
    }

    /**
     * 使用方法引用解析命名参数，但不绑定值。
     *
     * <pre>{@code
     * param(UserModel::getName) -> #{user_name}
     * }</pre>
     */
    public static <M, T> Param<T> named(SerializableFunction<M, T> getter) {
        return new Param<>(LambdaColumnResolver.resolve(getter), null, false);
    }

    @Override
    public String render(RenderContext ctx) {
        if (hasValue) {
            return ctx.bind(name, value);
        }
        return ctx.paramTemplate(name);
    }
}
