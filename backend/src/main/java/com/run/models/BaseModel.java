package com.run.models;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  23:26}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface BaseModel {
    void validate(String modelType, String modelName, Map<String, Object> modelCredential, Map<String, Object> other);
}
