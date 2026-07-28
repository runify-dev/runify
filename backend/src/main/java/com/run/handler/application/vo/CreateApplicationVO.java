package com.run.handler.application.vo;

import io.vertx.core.json.JsonObject;
import lombok.Data;

@Data
public class CreateApplicationVO {
    private String name;
    private String desc;
    private String icon;
    private JsonObject workflow;
    private Boolean allowAnonymousAccess;
    private String appType;
}
