package com.run.handler.user.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditUserVO {
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private String icon;
}
