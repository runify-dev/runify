package com.run.common.forms.field;

import com.run.common.forms.BaseField;
import com.run.common.forms.BaseLabel;
import com.run.common.forms.ShowRule;

import java.util.Map;

/**
 * 滑块字段
 */
public class SliderField extends BaseField {
    public SliderField(BaseLabel label,
                       Boolean required,
                       Object defaultValue,
                       ShowRule showRules,
                       Map<String, Object> attrs,
                       Map<String, Object> props) {
        super("Slider", label, required, defaultValue, showRules, attrs, props);
    }

    public SliderField(BaseLabel label,
                       Boolean required,
                       Object defaultValue) {
        super("Slider", label, required, defaultValue, null, null, null);
    }

    public SliderField(BaseLabel label,
                       Boolean required,
                       Object defaultValue,
                       Integer min,
                       Integer max,
                       Integer step) {
        super("Slider", label, required, defaultValue, null, Map.of("min", min, "max", max, "step", step), null);
    }

    @Override
    public void validate(Object value) {
        super.validate(value);
        if (value != null) {
            Map<String, Object> attrs = this.getAttrs();
            if (attrs != null) {
                Number numValue = (Number) value;
                if (attrs.containsKey("max")) {
                    Integer max = (Integer) attrs.get("max");
                    if (numValue.intValue() > max) {
                        throw new RuntimeException("字段值不能大于" + max);
                    }
                }
                if (attrs.containsKey("min")) {
                    Integer min = (Integer) attrs.get("min");
                    if (numValue.intValue() < min) {
                        throw new RuntimeException("字段值不能小于" + min);
                    }
                }
            }
        }
    }
}
