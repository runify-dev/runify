package com.run.dao.entity;

import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

@Table(schemaName = "public", name = "integration_relation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationRelation implements BaseEntity<IntegrationRelation> {
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
