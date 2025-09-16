package com.run.models;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  23:16}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public enum ModelType {
    LLM("LLM", "大语言模型", "com/run/models/icon/text-generation.svg");
    private String code;
    private String message;
    private String icon;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getIcon() {
        return icon;
    }

    ModelType(String code, String message, String iconPath) {
        this.code = code;
        this.message = message;
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(iconPath)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + iconPath);
            }
            this.icon = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
