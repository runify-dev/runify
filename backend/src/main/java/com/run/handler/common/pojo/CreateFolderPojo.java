package com.run.handler.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  11:58}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateFolderPojo {
    private String name;
    private String parentId;
    private String desc;
}
