package com.run.handler.model.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/9  22:47}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelEditPojo {
    private String provider;
    private String modelType;
    private String modelName;
    private JsonObject credential;
    private JsonArray modelParameterForm;

}
