package com.run.common.openai.request.completion_create_params;

import java.util.HashMap;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class Function {
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
    private HashMap<String, Object> parameters;
}
