package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.entity.BaseRelation;
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

@Table(schemaName = "public", name = "application_relation")
@Getter
@Setter
@AllArgsConstructor
public class ApplicationRelation extends BaseRelation implements BaseEntity<ApplicationRelation> {
    public ApplicationRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        setId(id);
        setAncestorId(ancestorId);
        setDescendantId(descendantId);
        setDepth(dept);
    }

    public static void main(String[] args) {
        ApplicationRelation applicationRelation = new ApplicationRelation();
        applicationRelation.setId(UUID.randomUUID());
        applicationRelation.setDepth(1);
        applicationRelation.setDescendantId(UUID.randomUUID());
        applicationRelation.setAncestorId(UUID.randomUUID());
        Map<String, Object> map = applicationRelation.getConvertMap().get(SQLDialect.SQLITE).toMap(applicationRelation);
        System.out.println(map);
    }

    public static WallNodeRelation<ApplicationRelation, Application> getWallNodeRelation() {
        return new WallNodeRelation<>() {
            @Override
            public ApplicationRelation apply(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
                return new ApplicationRelation(id, ancestorId, descendantId, dept);
            }

            @Override
            public Application build(UUID id, UUID parentId, String type, String name) {
                return new Application(id, parentId, name, type, "", new JsonObject(), new JsonObject(), false, false, LocalDateTime.now(), LocalDateTime.now());
            }

            @Override
            public UUID getAncestorId(ApplicationRelation applicationNodeRelation) {
                return applicationNodeRelation.getAncestorId();
            }

            @Override
            public UUID getDescendantId(ApplicationRelation applicationNodeRelation) {
                return applicationNodeRelation.getDescendantId();
            }

            @Override
            public Integer getDepth(ApplicationRelation applicationNodeRelation) {
                return applicationNodeRelation.getDepth();
            }

            @Override
            public UUID getParentId(Application application) {
                return application.getId();
            }

            @Override
            public void setParentId(Application application, UUID parentId) {
                application.setParentId(parentId);
            }

            @Override
            public UUID getId(Application application) {
                return application.getId();
            }

            @Override
            public String getName(Application application) {
                return application.getName();
            }

            @Override
            public Map<String, String> getNamePrefixMap() {
                return Map.of("md", "新建应用", "folder", "新建文件夹");
            }
        };
    }


    @Override
    @JsonIgnore
    public Map<SQLDialect, BaseConvert<ApplicationRelation>> getConvertMap() {
        return Map.of(SQLDialect.SQLITE, new Pgsql(),
                SQLDialect.POSTGRES, new Pgsql(),
                SQLDialect.H2, new Pgsql());
    }


    class Pgsql implements BaseConvert<ApplicationRelation> {
        @Override
        public ApplicationRelation mapTo(Row row) {
            return new ApplicationRelation(row.getUUID("id"),
                    row.getUUID("ancestor_id"),
                    row.getUUID("descendant_id"),
                    row.getInteger("depth"));

        }
    }


}
