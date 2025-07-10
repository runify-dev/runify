package com.run.models;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  22:53}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IProvider {
    /**
     * 供应商信息
     *
     * @return 供应商信息
     */
    ProvideInfo info();

    /**
     * 获取模型管理器
     *
     * @return 模型管理器
     */
    ModelInfoManage getModelInfoManage();

    default void validate(String modelType,
                          String modelName,
                          Map<String, Object> modelCredential,
                          Map<String, Object> other) {
        ModelInfo modelInfo = getModelInfo(modelType, modelName);
        modelInfo.getCredential().validate(modelCredential, modelType, modelName, this, other);

    }

    default ModelInfo getModelInfo(String modelType,
                                   String modelName) {
        ModelInfoManage modelInfoManage = getModelInfoManage();
        ModelType modelTypeV = ModelType.valueOf(modelType);
        return Optional
                .ofNullable(modelInfoManage.getModelDict()
                        .get(modelTypeV)
                        .get(modelName))
                .orElseGet(() -> modelInfoManage.getDefaultModelDict().get(modelTypeV));
    }

    default <T extends BaseModel> T getModel(String modelType,
                                             String modelName,
                                             Map<String, Object> modelCredential,
                                             Map<String, Object> other,
                                             Class<T> clazz
    ) {
        ModelInfo modelInfo = getModelInfo(modelType, modelName);
        Constructor<? extends BaseModel> constructor = null;
        try {
            constructor = modelInfo.getModelClass().getConstructor(String.class, String.class, Map.class, Map.class);
            return (T) constructor.newInstance(modelType, modelName, modelCredential, other);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }


    }

}
