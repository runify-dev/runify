package com.run.dao.common.entity;

import com.run.dao.common.annotations.Column;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/9/9  21:38}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class BaseTreeNode {
    /**
     * 应用id
     */
    @Column(name = "id", primaryKey = true)
    private UUID id;
    /**
     * 父id
     */
    @Column(name = "parent_id")
    private UUID parentId;
    /**
     * 节点类型
     */
    @Column(name = "type")
    private String type;
    /**
     * 加星
     */
    @Column(name = "star")
    private Boolean star;
    /**
     * 分享
     */
    @Column(name = "share")
    private Boolean share;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
