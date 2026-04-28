package com.run.sql.metadata;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型元数据解析器。
 *
 * <p>统一缓存模型类上的表信息和列信息，避免 insertInto(model)、set(model)、table(Model.class)
 * 这些入口重复解析 @Table / @Column 注解。</p>
 */
public final class ModelResolver {

    private static final ConcurrentHashMap<Class<?>, ModelMeta> MODEL_CACHE = new ConcurrentHashMap<>();

    private ModelResolver() {
    }

    public static ModelMeta resolve(Class<?> modelClass) {
        Objects.requireNonNull(modelClass, "modelClass");
        return MODEL_CACHE.computeIfAbsent(modelClass, ModelResolver::resolveUncached);
    }

    public static ModelMeta resolve(Object model) {
        Objects.requireNonNull(model, "model");
        return resolve(model.getClass());
    }

    public static int modelCacheSize() {
        return MODEL_CACHE.size();
    }

    public static void clearCache() {
        MODEL_CACHE.clear();
    }

    private static ModelMeta resolveUncached(Class<?> modelClass) {
        return new ModelMeta(
                TableResolver.resolve(modelClass),
                ModelColumnResolver.resolve(modelClass)
        );
    }
}
