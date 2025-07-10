package com.run.dao.entity;


import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/11  00:44}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
@Table(schemaName = "public", name = "node_relation")
@AllArgsConstructor
@NoArgsConstructor
public class NodeRelation implements BaseEntity<NodeRelation> {
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

    /**
     * 节点 source 用于冗余
     */
    @Column(name = "source")
    private String source;

    /**
     * 节点类型
     * 后代节点类型 用于冗余
     */
    @Column(name = "type")
    private String type;
    /**
     * 节点名称
     * 后代节点名称 用于冗余
     */
    @Column(name = "name")
    private String name;


    @Override
    public NodeRelation mapTo(Row row) {
        NodeRelation nodeRelation = new NodeRelation();
        nodeRelation.id = row.getUUID("id");
        nodeRelation.ancestorId = row.getUUID("ancestor_id");
        nodeRelation.descendantId = row.getUUID("descendant_id");
        nodeRelation.depth = row.getInteger("depth");
        nodeRelation.source = row.getString("source");
        nodeRelation.name = row.getString("name");
        return nodeRelation;
    }
}
