package com.run.common.constants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum ContentTypeConstants {
    /**
     * 文本
     */
    TEXT,
    /**
     * 思考过程
     */
    REASONING,
    /**
     *
     */
    QUESTION,
    /**
     * JSON
     */
    JSON,
    /**
     * 响应头
     */
    HEADERS,
    /**
     * json字段
     */
    JSON_FIELD,
    /**
     * 状态码
     */
    STATUS,
    /**
     * 失败
     */
    FAILURE,

    SYSTEM,
    /**
     * 工具调用
     */
    TOOL,
    /**
     * 跳过
     */
    CONTINUE,
    /**
     * 退出循环
     */
    BREAK,
    /**
     * 审批
     */
    APPROVAL,
    /**
     * 已提交
     */
    APPROVAL_SUBMIT;
    private static final Set<String> NAMES = Arrays.stream(values()).map(ContentTypeConstants::name).collect(Collectors.toSet());

    public static boolean contains(String name) {
        return NAMES.contains(name);
    }
}
