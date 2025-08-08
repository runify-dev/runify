package com.run.models;

import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  23:20}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface BaseModelCredential {
    /**
     * 校验
     *
     * @param modelCredential 模型参数
     * @param modelType       模型类型
     * @param modelName       模型名称
     * @param provider        供应商
     * @param other           其他参数
     */
    void validate(Map<String, Object> modelCredential, String modelType, String modelName, IProvider provider, Map<String, Object> other);

    /**
     * 加密数据
     *
     * @param credential 加密数据
     * @return 加密后数据
     */
    Map<String, Object> encryption(Map<String, Object> credential);


    List<Map<String, Object>> toFormList(Map<String, Object> keywords);
}
