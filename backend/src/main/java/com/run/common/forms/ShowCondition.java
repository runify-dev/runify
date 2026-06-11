package com.run.common.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class ShowCondition {
    private String field;
    private String compare;
    private Object value;

    public Map<String, Object> toMap() {
        if (value == null) {
            return Map.of("field", field, "compare", compare);
        }
        return Map.of("field", field, "compare", compare, "value", value);
    }
}
