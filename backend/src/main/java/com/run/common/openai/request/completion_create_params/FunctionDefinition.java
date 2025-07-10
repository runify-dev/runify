package com.run.common.openai.request.completion_create_params;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:13}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class FunctionDefinition {
    /**
     * 要调用的函数的名称。
     * 必须是a-z、a-z、0-9，或包含下划线和破折号，且具有最大长度
     * 64。
     */
    private String name;
    /**
     * 对函数功能的描述，由模型用于选择时间和
     * 如何调用函数。
     */
    private String description;
    /**
     * 函数接受的参数，描述为JSON模式对象。
     * 请参阅[指南](https://platform.openai.com/docs/guides/function-calling)for
     * 示例，以及
     * [JSON模式参考](https://json-schema.org/understanding-json-schema/)for
     * 关于格式的文档。
     * 省略“参数”定义了一个参数列表为空的函数。
     */
    private Map<String, Object> parameters;
    /**
     * 在生成函数调用时是否启用严格的模式遵从性。
     * 如果设置为true，模型将遵循
     * `参数字段。当“strict”为时，只支持JSON模式的一个子集
     * `真的”。在中了解有关结构化输出的更多信息
     * [函数调用指南]（文档/指南/函数调用）。
     */
    private Boolean strict;

    public static FunctionDefinition of(String name, String description, Map<String, Object> parameters, Boolean strict) {
        FunctionDefinition functionDefinition = new FunctionDefinition();
        functionDefinition.setName(name);
        functionDefinition.setDescription(description);
        functionDefinition.setParameters(parameters);
        functionDefinition.setStrict(strict);
        return functionDefinition;

    }
}