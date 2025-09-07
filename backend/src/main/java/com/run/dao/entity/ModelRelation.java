package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.entity.WallNodeRelation;
import io.vertx.core.json.JsonArray;
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

@Table(schemaName = "public", name = "model_relation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelRelation implements BaseEntity<ModelRelation> {
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


    public static WallNodeRelation<ModelRelation, Model> getWallNodeRelation() {
        return new WallNodeRelation<>() {
            @Override
            public ModelRelation apply(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
                return new ModelRelation(id, ancestorId, descendantId, dept);
            }

            @Override
            public Model build(UUID id, UUID parentId, String type, String name) {
                return new Model(id, parentId, type, name, "", "openai_provider", "LLM", "", "", new JsonArray(), new JsonObject(), false, false, LocalDateTime.now(), LocalDateTime.now());
            }

            @Override
            public UUID getAncestorId(ModelRelation knowledgeNodeRelation) {
                return knowledgeNodeRelation.getAncestorId();
            }

            @Override
            public UUID getDescendantId(ModelRelation knowledgeNodeRelation) {
                return knowledgeNodeRelation.getDescendantId();
            }

            @Override
            public Integer getDepth(ModelRelation knowledgeNodeRelation) {
                return knowledgeNodeRelation.getDepth();
            }

            @Override
            public UUID getParentId(Model model) {
                return model.getParentId();
            }

            @Override
            public void setParentId(Model model, UUID parentId) {
                model.setParentId(parentId);
            }

            @Override
            public UUID getId(Model model) {
                return model.getId();
            }

            @Override
            public String getName(Model model) {
                return model.getName();
            }

            @Override
            public Map<String, String> getNamePrefixMap() {
                return Map.of("md", "新建模型", "folder", "新建文件夹");
            }
        };
    }

    @Override
    @JsonIgnore
    public Map<SQLDialect, BaseConvert<ModelRelation>> getConvertMap() {
        return Map.of(SQLDialect.SQLITE, new Sqlite(),
                SQLDialect.POSTGRES, new Pgsql(),
                SQLDialect.H2, new Pgsql());
    }


    class Pgsql implements BaseConvert<ModelRelation> {
        @Override
        public ModelRelation mapTo(Row row) {
            return new ModelRelation(row.getUUID("id"),
                    row.getUUID("ancestor_id"),
                    row.getUUID("descendant_id"),
                    row.getInteger("depth"));
        }
    }

    class Sqlite implements BaseConvert<ModelRelation> {
        @Override
        public ModelRelation mapTo(Row row) {
            return new ModelRelation(row.getUUID("id"),
                    row.getUUID("ancestor_id"),
                    row.getUUID("descendant_id"),
                    row.getInteger("depth"));
        }
    }

}
