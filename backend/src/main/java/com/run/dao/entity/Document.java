package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(schemaName = "public", name = "document")
public class Document implements BaseEntity<Document> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "knowledge_id")
    private UUID knowledgeId;

    @Column(name = "name")
    private String name;

    @Column(name = "icon")
    private String icon;

    @Column(name = "content")
    private String content;

    @Column(name = "excerpt")
    private String excerpt;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
