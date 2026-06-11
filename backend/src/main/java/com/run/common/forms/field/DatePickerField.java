package com.run.common.forms.field;

import com.run.common.forms.BaseField;
import com.run.common.forms.BaseLabel;
import com.run.common.forms.ShowRule;

import java.util.Map;

/**
 * 日期选择字段
 */
public class DatePickerField extends BaseField {
    public DatePickerField(BaseLabel label,
                           Boolean required,
                           Object defaultValue,
                           ShowRule showRules,
                           Map<String, Object> attrs,
                           Map<String, Object> props) {
        super("DatePicker", label, required, defaultValue, showRules, attrs, props);
    }

    public DatePickerField(BaseLabel label,
                           Boolean required,
                           Object defaultValue) {
        super("DatePicker", label, required, defaultValue, null, null, null);
    }

    @Override
    public void validate(Object value) {
        super.validate(value);
    }
}
