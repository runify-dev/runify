package com.run.common.openai.request.message;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

public interface Message {

    String getRole();

    String getContent();

    default Map<String, Object> toMap() {
        return Map.of("role", this.getRole(), "content", this.getContent());
    }
}
