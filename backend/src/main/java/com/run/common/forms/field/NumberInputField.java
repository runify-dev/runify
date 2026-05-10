package com.run.common.forms.field;

import com.run.common.forms.BaseField;
import com.run.common.forms.BaseLabel;

import java.util.List;
import java.util.Map;

/**
 * 数字输入框字段
 */
public class NumberInputField extends BaseField {
    public NumberInputField(BaseLabel label,
                            Boolean required,
                            Object defaultValue,
                            Map<String, List<Object>> displayConstraint,
                            Map<String, Object> attrs,
                            Map<String, Object> props) {
        super("NumberInput", label, required, defaultValue, displayConstraint, attrs, props);
    }

    public NumberInputField(BaseLabel label,
                            Boolean required,
                            Object defaultValue) {
        super("NumberInput", label, required, defaultValue, null, null, null);
    }

    public NumberInputField(BaseLabel label,
                            Boolean required,
                            Object defaultValue,
                            Integer min,
                            Integer max) {
        super("NumberInput", label, required, defaultValue, null, Map.of("min", min, "max", max), null);
    }

    @Override
    public void validate(Object value) {
        super.validate(value);
        if (value != null) {
            Map<String, Object> attrs = this.getAttrs();
            if (attrs != null) {
                if (attrs.containsKey("max")) {
                    Integer max = (Integer) attrs.get("max");
                    Number numValue = (Number) value;
                    if (numValue.intValue() > max) {
                        throw new RuntimeException("字段值不能大于" + max);
                    }
                }
                if (attrs.containsKey("min")) {
                    Integer min = (Integer) attrs.get("min");
                    Number numValue = (Number) value;
                    if (numValue.intValue() < min) {
                        throw new RuntimeException("字段值不能小于" + min);
                    }
                }
            }
        }
    }
}
