package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.entity.WallNodeRelation;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jooq.SQLDialect;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/17  22:53}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

@Table(schemaName = "public", name = "knowledge_relation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRelation implements BaseEntity<KnowledgeRelation> {
    @Column(name = "id", primaryKey = true)
    private UUID id;
    /**
     * 祖先节点
     */
    @Column(name = "ancestor_id")
    private UUID ancestorId;
    /**
     * 后代节点
     */
    @Column(name = "descendant_id")
    private UUID descendantId;
    /**
     * 层级深度 祖先和后代的层级深度
     */
    @Column(name = "depth")
    private Integer depth;


    public static WallNodeRelation<KnowledgeRelation, Knowledge> getWallNodeRelation() {
        return new WallNodeRelation<>() {
            @Override
            public KnowledgeRelation apply(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
                return new KnowledgeRelation(id, ancestorId, descendantId, dept);
            }

            @Override
            public Knowledge build(UUID id, UUID parentId, String type, String name) {
                return new Knowledge(id, parentId, name, type, "", "", false, false, new JsonObject(), LocalDateTime.now(), LocalDateTime.now());
            }

            @Override
            public UUID getAncestorId(KnowledgeRelation knowledgeNodeRelation) {
                return knowledgeNodeRelation.getAncestorId();
            }

            @Override
            public UUID getDescendantId(KnowledgeRelation knowledgeNodeRelation) {
                return knowledgeNodeRelation.getDescendantId();
            }

            @Override
            public Integer getDepth(KnowledgeRelation knowledgeNodeRelation) {
                return knowledgeNodeRelation.getDepth();
            }

            @Override
            public UUID getParentId(Knowledge knowledgeNode) {
                return knowledgeNode.getParentId();
            }

            @Override
            public void setParentId(Knowledge knowledgeNode, UUID parentId) {
                knowledgeNode.setParentId(parentId);
            }

            @Override
            public UUID getId(Knowledge knowledgeNode) {
                return knowledgeNode.getId();
            }

            @Override
            public String getName(Knowledge knowledgeNode) {
                return knowledgeNode.getName();
            }

            @Override
            public Map<String, String> getNamePrefixMap() {
                return Map.of("md", "新建知识库", "folder", "新建文件夹");
            }
        };
    }

    @Override
    @JsonIgnore
    public Map<SQLDialect, BaseConvert<KnowledgeRelation>> getConvertMap() {
        return Map.of(SQLDialect.SQLITE, new Sqlite(),
                SQLDialect.POSTGRES, new Pgsql(),
                SQLDialect.H2, new Pgsql());
    }


    class Pgsql implements BaseConvert<KnowledgeRelation> {
        @Override
        public KnowledgeRelation mapTo(Row row) {
            return new KnowledgeRelation(row.getUUID("id"),
                    row.getUUID("ancestor_id"),
                    row.getUUID("descendant_id"),
                    row.getInteger("depth"));
        }
    }

    class Sqlite implements BaseConvert<KnowledgeRelation> {


        @Override
        public KnowledgeRelation mapTo(Row row) {
            return new KnowledgeRelation(row.getUUID("id"),
                    row.getUUID("ancestor_id"),
                    row.getUUID("descendant_id"),
                    row.getInteger("depth"));
        }
    }

}
