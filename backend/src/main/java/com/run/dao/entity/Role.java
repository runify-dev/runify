package com.run.dao.entity;

import com.run.auth.constants.PermissionConstants;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(schemaName = "public", name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role implements BaseEntity<Role> {
    @Column(name = "id", primaryKey = true)
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "internal")
    private Boolean internal;

    @Column(name = "type")
    private PermissionConstants.Role type;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
