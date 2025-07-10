package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.run.common.validator.Group;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.vertx.sqlclient.Row;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  18:31}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "markdown_node")
public class MarkdownNode implements BaseEntity<MarkdownNode> {
    /**
     * 节点id
     */
    @Column(name = "id", primaryKey = true)
    private UUID id;
    /**
     * 内容
     */
    @Column(name = "content")
    @NotBlank(message = "内容不能为空", groups = Group.Edit.class)
    private String content;

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
    public MarkdownNode mapTo(Row row) {
        MarkdownNode markdownNode = new MarkdownNode();
        markdownNode.id = row.getUUID("id");
        markdownNode.content = row.getString("content");
        markdownNode.createTime = row.getLocalDateTime("create_time");
        markdownNode.updateTime = row.getLocalDateTime("update_time");
        return markdownNode;
    }

    public static Schema<?> getSchema() {
        return new JsonSchema().addProperty("id", new StringSchema().description("节点id"))
                .addProperty("content", new StringSchema().description("节点内容"))
                .addProperty("createTime", new StringSchema().description("创建时间"))
                .addProperty("updateTime", new StringSchema().description("修改时间"));
    }
}
