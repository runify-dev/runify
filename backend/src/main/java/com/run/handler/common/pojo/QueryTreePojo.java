package com.run.handler.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  10:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryTreePojo {
    /**
     * 父id
     */
    private String parentId;
    /**
     * 深度
     */
    private Integer depth;
}
