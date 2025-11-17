package com.run.handler.common.pojo;

import com.run.dao.common.annotations.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  16:53}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleNodePojo {
    private UUID id;

    private UUID parentId;

    private String type;

    private String name;

    private String desc;
}
