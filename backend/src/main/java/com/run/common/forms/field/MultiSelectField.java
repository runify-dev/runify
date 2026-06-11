package com.run.common.forms.field;

import com.run.common.forms.BaseField;
import com.run.common.forms.BaseLabel;
import com.run.common.forms.ShowRule;

import java.util.List;
import java.util.Map;

/**
 * 多选下拉字段
 */
public class MultiSelectField extends BaseField {

    private List<Object> optionList;
    private String labelField;
    private String valueField;

    public MultiSelectField(BaseLabel label,
                            Boolean required,
                            Object defaultValue,
                            List<Object> optionList,
                            String labelField,
                            String valueField,
                            ShowRule showRules,
                            Map<String, Object> attrs,
                            Map<String, Object> props) {
        super("MultiSelect", label, required, defaultValue, showRules, attrs, props);
        this.optionList = optionList;
        this.labelField = labelField;
        this.valueField = valueField;
    }

    public MultiSelectField(BaseLabel label,
                            Boolean required,
                            Object defaultValue,
                            List<Object> optionList) {
        super("MultiSelect", label, required, defaultValue, null, null, null);
        this.optionList = optionList;
        this.labelField = "label";
        this.valueField = "value";
    }

    public MultiSelectField(BaseLabel label,
                            Boolean required,
                            Object defaultValue,
                            List<Object> optionList,
                            String labelField,
                            String valueField) {
        super("MultiSelect", label, required, defaultValue, null, null, null);
        this.optionList = optionList;
        this.labelField = labelField;
        this.valueField = valueField;
    }

    @Override
    public Map<String, Object> toMap(Map<String, Object> keywords) {
        Map<String, Object> result = super.toMap(keywords);
        result.put("optionList", optionList == null ? List.of() : optionList);
        result.put("labelField", labelField == null ? "label" : labelField);
        result.put("valueField", valueField == null ? "value" : valueField);
        return result;
    }

    @Override
    public void validate(Object value) {
        super.validate(value);
    }
}
