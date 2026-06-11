package com.run.common.forms.field;

import com.run.common.forms.BaseField;
import com.run.common.forms.BaseLabel;
import com.run.common.forms.ShowRule;

import java.util.Map;

/**
 * JSON 输入框字段
 */
public class JsonInputField extends BaseField {
    public JsonInputField(BaseLabel label,
                          Boolean required,
                          Object defaultValue,
                          ShowRule showRules,
                          Map<String, Object> attrs,
                          Map<String, Object> props) {
        super("JsonInput", label, required, defaultValue, showRules, attrs, props);
    }

    public JsonInputField(BaseLabel label,
                          Boolean required,
                          Object defaultValue) {
        super("JsonInput", label, required, defaultValue, null, null, null);
    }

    @Override
    public void validate(Object value) {
        super.validate(value);
    }
}
