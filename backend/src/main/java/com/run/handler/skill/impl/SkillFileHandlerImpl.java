package com.run.handler.skill.impl;

import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Skill;
import com.run.dao.entity.SkillFile;
import com.run.dao.mapper.FileMapper;
import com.run.dao.mapper.SkillFileMapper;
import com.run.dao.mapper.SkillMapper;
import com.run.handler.skill.ISkillFileHandler;
import com.run.sql.DSL;
import com.run.common.exception.ApiException;
import com.run.common.result.Result;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SkillFileHandlerImpl implements ISkillFileHandler {
    private final SkillFileMapper skillFileMapper;
    private final FileMapper fileMapper;
    private final SkillMapper skillMapper;

    @Inject
    public SkillFileHandlerImpl(SkillFileMapper skillFileMapper, FileMapper fileMapper, SkillMapper skillMapper) {
        this.skillFileMapper = skillFileMapper;
        this.fileMapper = fileMapper;
        this.skillMapper = skillMapper;
    }

    /**
     * 同步更新父 Skill 的 update_time
     */
    private Future<Void> touchSkill(String skillId) {
        return skillMapper.update(
                Map.of(DSL.field("update_time"), DSL.param("update_time")),
                DSL.field("id").eq(DSL.param("id")),
                Map.of("update_time", LocalDateTime.now(), "id", skillId)
        ).mapEmpty();
    }

    @Override
    public void listByParent(RoutingContext context) {
        String skillId = context.pathParam("resourceId");
        String parentId = context.pathParam("fileId");
        skillFileMapper.list(
                DSL.field("skill_id").eq(DSL.param("skillId"))
                        .and(DSL.field("parent_id").eq(DSL.param("parentId"))),
                Map.of("skillId", skillId, "parentId", parentId)
        ).onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void tree(RoutingContext context) {
        String skillId = context.pathParam("resourceId");
        skillFileMapper.list(
                DSL.field("skill_id").eq(DSL.param("skillId")),
                Map.of("skillId", skillId)
        ).onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void get(RoutingContext context) {
        String fileId = context.pathParam("fileId");
        skillFileMapper.getById(fileId)
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    private Future<Boolean> checkNameUnique(String skillId, String parentId, String name, String excludeId) {
        var condition = DSL.field("skill_id").eq(DSL.param("skillId"))
                .and(DSL.field("parent_id").eq(DSL.param("parentId")))
                .and(DSL.field("name").eq(DSL.param("name")));
        Map<String, Object> params = Map.of("skillId", skillId, "parentId", parentId, "name", name);
        if (StringUtils.isNotEmpty(excludeId)) {
            condition = condition.and(DSL.field("id").ne(DSL.param("excludeId")));
            params = Map.of("skillId", skillId, "parentId", parentId, "name", name, "excludeId", excludeId);
        }
        final var finalCondition = condition;
        final var finalParams = params;
        return skillFileMapper.list(finalCondition, finalParams).compose(list -> {
            if (!list.isEmpty()) {
                return Future.failedFuture(ApiException.of(400, "同级目录已存在同名文件: " + name));
            }
            return Future.succeededFuture(true);
        });
    }

    @Override
    public void createFolder(RoutingContext context) {
        String skillId = context.pathParam("resourceId");
        String parentId = context.pathParam("fileId");
        String name = context.body().asJsonObject().getString("name", "新建文件夹");
        LocalDateTime now = LocalDateTime.now();
        checkNameUnique(skillId, parentId, name, null)
                .compose(_ -> {
                    SkillFile folder = new SkillFile(
                            UUID.randomUUID(),
                            UUID.fromString(parentId),
                            UUID.fromString(skillId),
                            name, "folder", null, null, null, null, null, now, now
                    );
                    return skillFileMapper.save(folder).compose(_ -> touchSkill(skillId)).map(folder);
                })
                .onSuccess(folder -> context.end(Result.success(folder).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void createText(RoutingContext context) {
        String skillId = context.pathParam("resourceId");
        String parentId = context.pathParam("fileId");
        String name = context.body().asJsonObject().getString("name", "新建文本.md");
        LocalDateTime now = LocalDateTime.now();
        checkNameUnique(skillId, parentId, name, null)
                .compose(_ -> {
                    SkillFile textFile = new SkillFile(
                            UUID.randomUUID(),
                            UUID.fromString(parentId),
                            UUID.fromString(skillId),
                            name, "text", "", null, null, null, null, now, now
                    );
                    return skillFileMapper.save(textFile).compose(_ -> touchSkill(skillId)).map(textFile);
                })
                .onSuccess(textFile -> context.end(Result.success(textFile).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void uploadFile(RoutingContext context) {
        String skillId = context.pathParam("resourceId");
        String parentId = context.pathParam("fileId");
        var uploads = context.fileUploads();
        if (uploads.isEmpty()) {
            context.fail(400);
            return;
        }
        var upload = uploads.get(0);
        java.io.File file = new java.io.File(upload.uploadedFileName());
        String fileName = upload.fileName();

        checkNameUnique(skillId, parentId, fileName, null)
                .compose(_ -> fileMapper.upload(fileName, upload.size(), null, null, file))
                .onSuccess(fileEntity -> {
                    LocalDateTime now = LocalDateTime.now();
                    SkillFile skillFile = new SkillFile(
                            UUID.randomUUID(),
                            UUID.fromString(parentId),
                            UUID.fromString(skillId),
                            fileName, "file", null,
                            fileEntity.getId(),
                            fileEntity.getFileName(),
                            fileEntity.getSize(),
                            null, now, now
                    );
                    skillFileMapper.save(skillFile)
                            .compose(_ -> touchSkill(skillId))
                            .onSuccess(_ -> context.end(Result.success(skillFile).toBuffer()))
                            .onFailure(context::fail);
                })
                .onFailure(context::fail);
    }

    @Override
    public void updateContent(RoutingContext context) {
        String fileId = context.pathParam("fileId");
        String content = context.body().asJsonObject().getString("content", "");
        skillFileMapper.update(
                Map.of(DSL.field("content"), DSL.param("content"),
                        DSL.field("update_time"), DSL.param("update_time")),
                DSL.field("id").eq(DSL.param("id")),
                Map.of("content", content, "update_time", LocalDateTime.now(), "id", fileId)
        ).compose(_ -> skillFileMapper.getById(fileId))
                .compose(file -> touchSkill(file.getSkillId().toString()).map(file))
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void rename(RoutingContext context) {
        String fileId = context.pathParam("fileId");
        String name = context.body().asJsonObject().getString("name");
        skillFileMapper.getById(fileId)
                .compose(existing -> checkNameUnique(
                        existing.getSkillId().toString(),
                        existing.getParentId() != null ? existing.getParentId().toString() : "00000000-0000-0000-0000-000000000000",
                        name, fileId))
                .compose(_ -> skillFileMapper.update(
                        Map.of(DSL.field("name"), DSL.param("name"),
                                DSL.field("update_time"), DSL.param("update_time")),
                        DSL.field("id").eq(DSL.param("id")),
                        Map.of("name", name, "update_time", LocalDateTime.now(), "id", fileId)))
                .compose(_ -> skillFileMapper.getById(fileId))
                .compose(file -> touchSkill(file.getSkillId().toString()).map(file))
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void delete(RoutingContext context) {
        String fileId = context.pathParam("fileId");
        skillFileMapper.getById(fileId)
                .compose(skillFile -> {
                    String skillId = skillFile.getSkillId().toString();
                    if ("folder".equals(skillFile.getType())) {
                        return deleteRecursively(fileId).compose(_ -> touchSkill(skillId)).map(true);
                    }
                    return skillFileMapper.deleteById(fileId).compose(_ -> touchSkill(skillId)).map(true);
                })
                .onSuccess(_ -> context.end(Result.success(true).toBuffer()))
                .onFailure(context::fail);
    }

    private Future<Boolean> deleteRecursively(String folderId) {
        return skillFileMapper.list(
                DSL.field("parent_id").eq(DSL.param("parentId")),
                Map.of("parentId", folderId)
        ).compose(children -> {
            List<Future<?>> futures = new ArrayList<>();
            for (SkillFile child : children) {
                if ("folder".equals(child.getType())) {
                    futures.add(deleteRecursively(child.getId().toString()));
                } else {
                    futures.add(skillFileMapper.deleteById(child.getId().toString()));
                }
            }
            return Future.all(futures).compose(_ -> skillFileMapper.deleteById(folderId).map(true));
        });
    }
}
