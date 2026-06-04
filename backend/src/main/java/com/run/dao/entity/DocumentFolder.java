package com.run.dao.entity;

import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "document_folder")
public class DocumentFolder implements BaseEntity<DocumentFolder> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "knowledge_id")
    private UUID knowledgeId;

    @Column(name = "name")
    private String name;

    @Column(name = "desc")
    private String desc;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
