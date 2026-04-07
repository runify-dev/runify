package com.run.workflow.converter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  17:41}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public enum ContentConvertStrategy {
    /**
     * 忽略，不传给 OpenAI
     */
    IGNORE,
    /**
     * 提取文本直接拼入
     */
    TEXT,
    /**
     * 加前缀后拼入
     */
    PREFIXED
}