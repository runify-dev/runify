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

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  20:27}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "model_folder")
public class ModelFolder implements BaseEntity<ModelFolder> {
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
     * 文件夹名称
     */
    @Column(name = "name")
    private String name;

    /**
     * 文件夹描述
     */
    @Column(name = "desc")
    private String desc;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;


}
