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
@Table(schemaName = "public", name = "skill_file")
public class SkillFile implements BaseEntity<SkillFile> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "skill_id")
    private UUID skillId;

    @Column(name = "name")
    private String name;

    /**
     * folder / text / file
     */
    @Column(name = "type")
    private String type;

    /**
     * 文本内容（type=text 时使用）
     */
    @Column(name = "content")
    private String content;

    /**
     * 关联的文件 id（type=file 时使用）
     */
    @Column(name = "file_id")
    private UUID fileId;

    /**
     * 原始文件名（type=file 时使用）
     */
    @Column(name = "file_name")
    private String fileName;

    /**
     * 文件大小（type=file 时使用）
     */
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "desc")
    private String desc;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
