package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.constants.RefType;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/29  20:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "file")
public class FileEntity implements BaseEntity<FileEntity> {
    /**
     * 节点id
     */
    @Column(name = "id", primaryKey = true)
    private UUID id;
    /**
     * 文件名称
     */
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "path")
    private String path;

    @Column(name = "size")
    private Long size;
    /**
     * pgsql大对象id
     */
    @Column(name = "lo_id")
    private Long loId;
    /**
     * 文件 sha1值
     */
    @Column(name = "sha256_hash")
    private String sha256Hash;
    /**
     * 应用当前文件的资源类型
     */
    @Column(name = "ref_type")
    private RefType refType;
    /**
     * 应用当前文件的资源唯一标识
     */
    @Column(name = "ref")
    private String ref;

    @Column(name = "meta")
    private JsonObject meta;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


}
