package com.run.handler.application.vo;

import lombok.Data;

@Data
public class CreateApplicationVO {
    private String name;
    private String desc;
    private String icon;
    private Boolean allowAnonymousAccess;
}
