package com.run.handler.skill.pojo;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditSkillPojo {
    private String name;
    private String icon;
    private String desc;
    private JsonObject parameterValue;
    private JsonArray skillParameterForm;
}
