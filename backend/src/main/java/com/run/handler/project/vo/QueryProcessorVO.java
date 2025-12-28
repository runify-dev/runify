package com.run.handler.project.vo;

import com.run.common.query.annotations.QueryParams;
import com.run.common.query.constants.LocationConstants;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/21  18:59}
 * {@code @Version 1.0}
 * {@code @注释:  }
 */
@Getter
@Setter
@NoArgsConstructor
public class QueryProcessorVO {
    @QueryParams(name = "projectId", location = LocationConstants.PATH)
    private UUID projectId;
    /**
     * 名称
     */
    @QueryParams(name = "name")
    private String name;
    /**
     * 描述
     */
    @QueryParams(name = "desc")
    private String desc;
    /**
     * 协议
     */
    @QueryParams(name = "protocol")
    private String protocol;
    /**
     * 当前页
     */
    @QueryParams(name = "currentPage")
    @NotNull(message = "当前页不能为空")
    private Long currentPage;
    /**
     * 每页大小
     */
    @QueryParams(name = "pageSize")
    @NotNull(message = "每页大小不能为空")
    private Long pageSize;
}
