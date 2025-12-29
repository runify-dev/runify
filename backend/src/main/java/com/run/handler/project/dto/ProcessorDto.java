package com.run.handler.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.run.common.constants.ProcessorProtocolConstants;
import com.run.dao.common.annotations.Column;
import io.vertx.core.json.JsonObject;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/29  20:49}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class ProcessorDto {

    private UUID id;


    private UUID projectId;

    private String name;

    private String desc;


    private ProcessorProtocolConstants protocol;


    private Boolean activate;

    private JsonObject meta;


    private JsonObject workflow;
    /**
     * 是否已部署
     */
    private Boolean isDeploy;
    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
