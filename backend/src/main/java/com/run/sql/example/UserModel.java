package com.run.sql.example;

import com.run.dao.common.annotations.Column;

@com.run.dao.common.annotations.Table(catalogName = "mydb", schemaName = "public", name = "user")
public class UserModel {
    @Column(name = "id", primaryKey = true)
    private String id;

    @Column(name = "user_name")
    private String name;

    @Column(name = "create_time")
    private Long createTime;

    public UserModel() {
    }

    public UserModel(String id, String name, Long createTime) {
        this.id = id;
        this.name = name;
        this.createTime = createTime;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getCreateTime() {
        return createTime;
    }
}
