package com.run.common.forms.field;

import com.run.common.forms.BaseField;
import com.run.common.forms.BaseLabel;

import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  21:44}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class PasswordInputField extends BaseField {
    public PasswordInputField(BaseLabel label,
                              Boolean required,
                              Object defaultValue,
                              Map<String, List<Object>> displayConstraint,
                              Map<String, Object> attrs,
                              Map<String, Object> props) {
        super("PasswordInputField", label, required, defaultValue, displayConstraint, attrs, props);
    }

    public PasswordInputField(BaseLabel label,
                              Boolean required,
                              Object defaultValue) {
        super("PasswordInputField", label, required, defaultValue, null, null, null);
    }

    @Override
    public void validate(Object value) {
        super.validate(value);
        Map<String, Object> attrs = this.getAttrs();
        if (attrs != null) {
            if (attrs.containsKey("maxlength")) {
                Integer maxlength = (Integer) attrs.get("maxlength");
                if (((String) value).length() > maxlength) {
                    throw new RuntimeException("字段长度不能大于" + maxlength);
                }
            }
            if (attrs.containsKey("minlength")) {
                Integer minlength = (Integer) attrs.get("minlength");
                if (((String) value).length() < minlength) {
                    throw new RuntimeException("字段长度不能小于" + minlength);
                }
            }
        }
    }
}
