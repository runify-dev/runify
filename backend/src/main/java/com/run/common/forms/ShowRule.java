package com.run.common.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class ShowRule {
    private String condition;
    private List<ShowCondition> conditions;

    public Map<String, Object> toMap() {
        return Map.of(
                "condition", condition,
                "conditions", conditions.stream().map(ShowCondition::toMap).toList());
    }
}
