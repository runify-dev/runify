package com.run.handler.tree.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/19  17:17}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSimpleNodePojo {
    /**
     * 节点名
     */
    private String name;

    private String type;
}
