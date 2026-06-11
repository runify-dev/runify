package com.run.common.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
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
    private String type;
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
     * 显示规则，条件满足时才显示该字段
     */
    private ShowRule showRules;
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
                        ShowRule showRules,
                        Map<String, Object> attrs,
                        Map<String, Object> props) {
        return new BaseField(inputType, label, required, defaultValue, showRules, attrs, props);
    }

    public void validate(Object value) {
        if (this.required && value == null) {
            throw new RuntimeException("字段%s是必填".formatted(this.label.getValue()));
        }
    }

    public Map<String, Object> toMap(Map<String, Object> keywords) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", this.type);
        result.put("label", this.label.toMap(Map.of()));
        result.put("required", this.required);
        result.put("defaultValue", this.defaultValue);
        if (this.showRules != null) {
            result.put("showRules", this.showRules.toMap());
        }
        result.put("attrs", this.attrs == null ? Map.of() : this.attrs);
        result.put("propsInfo", this.props == null ? Map.of() : this.props);
        result.put("showDefaultValue", true);
        return result;
    }


}
