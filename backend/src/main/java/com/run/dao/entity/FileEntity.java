package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.run.common.constants.DatabaseType;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.impl.JsonUtil;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
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
    private String refType;
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


    @Override
    @JsonIgnore
    public Map<DatabaseType, BaseConvert<FileEntity>> getConvertMap() {
        return Map.of(DatabaseType.SQLITE, new Sqlite(),
                DatabaseType.POSTGRESQL, new Pgsql());
    }


    class Pgsql implements BaseConvert<FileEntity> {
        @Override
        public FileEntity mapTo(Row row) {
            FileEntity file = new FileEntity();
            file.id = row.getUUID("id");
            file.fileName = row.getString("file_name");
            file.loId = row.getLong("lo_id");
            file.sha256Hash = row.getString("sha256_hash");
            file.refType = row.getString("ref_type");
            file.ref = row.getString("ref");
            file.meta = row.getJsonObject("meta");
            file.size = row.getLong("size");
            return file;
        }
    }

    class Sqlite implements BaseConvert<FileEntity> {
        @Override
        public FileEntity mapTo(Row row) {
            FileEntity file = new FileEntity();
            file.id = row.getUUID("id");
            file.fileName = row.getString("file_name");
            file.loId = row.getLong("lo_id");
            file.sha256Hash = row.getString("sha256_hash");
            file.refType = row.getString("ref_type");
            file.ref = row.getString("ref");
            file.meta = JacksonUtils.fromJson(row.getString("meta"), JsonObject.class);
            file.size = row.getLong("size");
            file.path = row.getString("path");
            return file;
        }
    }


}
