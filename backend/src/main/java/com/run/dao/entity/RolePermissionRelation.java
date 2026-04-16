package com.run.dao.entity;

import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Table(schemaName = "public", name = "role_permission_relation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionRelation implements BaseEntity<RolePermissionRelation> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "role_id")
    private String roleId;

    @Column(name = "permission_id")
    private String permissionId;
}
