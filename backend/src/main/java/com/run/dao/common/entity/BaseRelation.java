package com.run.dao.common.entity;

import com.run.dao.common.annotations.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/9/9  21:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class BaseRelation {
    @Column(name = "id", primaryKey = true)
    private UUID id;
    /**
     * 祖先节点
     */
    @Column(name = "ancestor_id")
    private UUID ancestorId;
    /**
     * 后代节点
     */
    @Column(name = "descendant_id")
    private UUID descendantId;
    /**
     * 层级深度 祖先和后代的层级深度
     */
    @Column(name = "depth")
    private Integer depth;
}
