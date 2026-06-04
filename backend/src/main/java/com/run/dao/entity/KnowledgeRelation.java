package com.run.dao.entity;

import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Table(schemaName = "public", name = "knowledge_relation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRelation implements BaseEntity<KnowledgeRelation> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "ancestor_id")
    private UUID ancestorId;

    @Column(name = "descendant_id")
    private UUID descendantId;

    @Column(name = "depth")
    private Integer depth;
}
