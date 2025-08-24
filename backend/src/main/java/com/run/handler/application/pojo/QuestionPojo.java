package com.run.handler.application.pojo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/23  17:56}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class QuestionPojo extends JsonObject {
    private String question;


    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.putAll(this.getMap());
        return result;
    }

}
