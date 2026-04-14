package com.run.common.query.vo;

import com.run.common.query.annotations.QueryParams;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/14  21:22}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@NoArgsConstructor
public class BasePageQueryVO {
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
