package com.run.handler.conversation.dto;

import com.fasterxml.jackson.databind.util.BeanUtil;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Application;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ApplicationDTO {
    private UUID id;
    private String name;
    private String desc;
    private String icon;
    private Boolean allowAnonymousAccess;

    public ApplicationDTO(Application application) {
        CommonUtils.copyProperties(application, this);
    }
}
