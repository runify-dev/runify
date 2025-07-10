package com.run.models;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  23:16}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public enum ModelType {
    LLM("LLMCredential", "大语言模型");
    private String code;
    private String message;

    ModelType(String code, String message) {
        this.code = code;
        this.message = message;
    }


}
