package com.run.common.forms.field;

import com.run.common.forms.BaseField;
import com.run.common.forms.BaseLabel;
import com.run.common.forms.ShowRule;

import java.util.Map;

/**
 * 开关字段
 */
public class SwitchInputField extends BaseField {
    public SwitchInputField(BaseLabel label,
                            Boolean required,
                            Object defaultValue,
                            ShowRule showRules,
                            Map<String, Object> attrs,
                            Map<String, Object> props) {
        super("SwitchInput", label, required, defaultValue, showRules, attrs, props);
    }

    public SwitchInputField(BaseLabel label,
                            Boolean required,
                            Object defaultValue) {
        super("SwitchInput", label, required, defaultValue, null, null, null);
    }

    @Override
    public void validate(Object value) {
        super.validate(value);
    }
}
