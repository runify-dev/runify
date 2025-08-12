package com.run.models.impl.deepseek.model;

import com.run.models.BaseOpenaiChatModel;

import java.util.Map;

/**
 * {@code @Author:guguli}
 * {@code @Date: 2025/08/12  22:00}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class LLM extends BaseOpenaiChatModel {
    public LLM(String baseUrl, String apiKey, String model) {
        super(baseUrl, apiKey, model);
    }

    public LLM(String modelType, String modelName, Map<String, Object> modelCredential, Map<String, Object> other) {
        super((String) modelCredential.get("baseUrl"), (String) modelCredential.get("apiKey"), modelName);
    }


}
