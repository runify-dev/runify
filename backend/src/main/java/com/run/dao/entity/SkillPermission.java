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

@Table(schemaName = "public", name = "skill_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillPermission implements BaseEntity<SkillPermission> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "target")
    private UUID target;

    @Column(name = "permission")
    private String permission;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
