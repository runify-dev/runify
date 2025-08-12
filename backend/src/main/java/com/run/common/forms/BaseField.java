package com.run.common.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  21:19}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
public class BaseField {
    /**
     * 主键类型
     */
    private String inputType;
    /**
     * label
     */
    private BaseLabel label;
    /**
     * 是否必填
     */
    private Boolean required;
    /**
     * 默认值
     */
    private Object defaultValue;
    /**
     * 显示限制 当前field什么时候显示
     * 下面是一个name字段有值的时候渲染实例
     * {
     * "name":null
     * }
     * 下面是一个name字段有值 并且是"张三"的时候才显示
     * {
     * "name":["张三"]
     * }
     */
    private Map<String, List<Object>> displayConstraint;

    /**
     * vue 属性
     */
    private Map<String, Object> attrs;
    /**
     * vue props参数
     */
    private Map<String, Object> props;

    public BaseField of(String inputType,
                        BaseLabel label,
                        Boolean required,
                        Object defaultValue,
                        Map<String, List<Object>> displayConstraint,
                        Map<String, Object> attrs,
                        Map<String, Object> props) {
        return new BaseField(inputType, label, required, defaultValue, displayConstraint, attrs, props);
    }

    public void validate(Object value) {
        if (this.required && value == null) {
            throw new RuntimeException("字段%s是必填".formatted(this.label.getValue()));
        }
    }

    public Map<String, Object> toMap(Map<String, Object> keywords) {
        return Map.of(
                "input_type", this.inputType,
                "label", this.label.toMap(Map.of()),
                "required", this.required,
                "default_value", this.defaultValue,
                "display_constraint", this.displayConstraint == null ? Map.of() : this.displayConstraint,
                "attrs", this.attrs == null ? Map.of() : this.attrs,
                "props_info", this.props == null ? Map.of() : this.props,
                "show_default_value", true);

    }


}
