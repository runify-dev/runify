package com.run.handler.project.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/21  18:59}
 * {@code @Version 1.0}
 * {@code @注释:  }
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateProcessorVO {
    /**
     * 名称
     */
    private String name;
    /**
     * 描述
     */
    private String desc;
    /**
     * 协议
     */
    private String protocol;
}
