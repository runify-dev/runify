package com.run.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

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

    public ModelInfo(String name, String desc, ModelType modelType, BaseModelCredential credential, Class<? extends BaseModel> modelClass) {
        this.name = name;
        this.desc = desc;
        this.modelType = modelType;
        this.credential = credential;
        this.modelClass = modelClass;
        this.isDefault = Boolean.FALSE;
    }

    private Boolean isDefault;

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("desc", desc);
        result.put("modelType", modelType);
        result.put("credential", credential.toFormList(Map.of()));
        result.put("isDefault", isDefault);
        return result;
    }

}
