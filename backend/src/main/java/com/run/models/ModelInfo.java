package com.run.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  23:14}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModelInfo {
    /**
     * 模型名称
     */
    private String name;
    /**
     * 模型描述
     */
    private String desc;
    /**
     * 模型类型
     */
    private ModelType modelType;
    /**
     * 认证对象
     */
    private BaseModelCredential credential;
    /**
     * 模型class
     */
    private Class<? extends BaseModel> modelClass;

}
