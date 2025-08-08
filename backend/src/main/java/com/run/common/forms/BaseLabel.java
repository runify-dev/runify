package com.run.common.forms;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  21:19}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@AllArgsConstructor
@Setter
@Getter
public class BaseLabel {
    /**
     * 组建类型
     */
    private String inputType;
    /**
     * 值
     */
    private String value;
    /**
     * vue 属性
     */
    private Map<String, Object> attrs;
    /**
     * vue props参数
     */
    private Map<String, Object> props;

    public Map<String, Object> toMap(Map<String, Object> keywords) {
        return Map.of(
                "input_type", this.inputType,
                "value", this.value,
                "attrs", this.attrs == null ? Map.of() : this.attrs,
                "props", this.props == null ? Map.of() : this.props);

    }
}
