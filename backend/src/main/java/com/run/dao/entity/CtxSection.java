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
 * 便签子区配置（应用级）：一个应用一套，定义有哪些便签子区、显示标题、抽取说明，
 * 以及每类便签的归属 scope（user / conversation / application）——即"这个便签跟用户、那个跟对话"。
 * <p>
 * 是"便签设置"菜单的存储；save 节点据此把便签落到对应 scope，抽取/渲染据此取 description/label。
 * 应用未配置时回退内置默认（见 SectionRegistry.BUILTIN_DEFAULTS）。
 * 唯一键 (application_id, section_key)。
 */
@Table(schemaName = "public", name = "ctx_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CtxSection implements BaseEntity<CtxSection> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "application_id")
    private String applicationId;

    /**
     * 子区标识（= 便签 subtype，存库），如 convention/preference/env/goal/todo 或自定义
     */
    @Column(name = "section_key")
    private String sectionKey;

    /**
     * 显示标题，如 约定/喜好
     */
    @Column(name = "label")
    private String label;

    /**
     * 抽取说明：告诉模型"抽什么"（自定义子区能被 AI 抽取的关键）
     */
    @Column(name = "description")
    private String description;

    /**
     * 归属 scope：user | conversation | application
     */
    @Column(name = "scope")
    private String scope;

    /**
     * 列表型（数组元素为整条字符串，如 goal/todo）；否则 {key,value} 型
     */
    @Column(name = "list_style")
    private Boolean listStyle;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
