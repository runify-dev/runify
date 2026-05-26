package com.run.handler.model.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelCreateVO {
    private String provider;
    private String modelType;
    private String modelName;
    private String desc;
    private JsonObject credential;
    private JsonArray modelParameterForm;
}
