package com.run.common.forms;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/25  22:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class BaseForm {
    public void validate(Map<String, Object> formData) {
        List<Field> fields = Arrays.stream(FieldUtils.getAllFields(this.getClass())).filter(field -> BaseField.class.isAssignableFrom(field.getType())).toList();
        for (Field field : fields) {
            try {
                BaseField baseField = (BaseField) FieldUtils.readField(field, this, true);
                baseField.validate(formData.get(field.getName()));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<Map<String, Object>> toFormList(Map<String, Object> keywords) {
        return Arrays.stream(FieldUtils.getAllFields(this.getClass())).filter(field -> BaseField.class.isAssignableFrom(field.getType())).map(field -> toMap(field, keywords)).toList();
    }

    private Map<String, Object> toMap(Field field, Map<String, Object> keywords) {
        try {
            BaseField baseField = (BaseField) FieldUtils.readField(field, this, true);
            HashMap<String, Object> result = new HashMap<>(baseField.toMap(keywords));
            result.put("field", field.getName());
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
