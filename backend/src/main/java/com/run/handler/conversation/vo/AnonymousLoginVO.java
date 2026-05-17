package com.run.handler.conversation.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/10  17:06}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class AnonymousLoginVO {
    @NotBlank(message = "浏览器指纹不能为空")
    private String visitorId;
}
