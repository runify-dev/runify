package com.run.handler.common.impl;

import com.run.common.exception.ApiException;
import com.run.common.result.Result;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.mapper.BaseMapper;
import com.run.handler.common.IFolderHandler;
import com.run.handler.common.Tool;
import com.run.handler.common.pojo.CreateFolderPojo;
import com.run.handler.common.pojo.QueryFolderPojo;
import com.run.sql.DSL;
import com.run.sql.condition.Condition;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;


import java.util.Map;
import java.util.UUID;

import static com.run.common.constants.CommonConstants.ROOT_FOLDER_ID_STR;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  17:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public abstract class FolderHandlerImpl<F extends BaseEntity<F>,
        Relation extends BaseEntity<Relation>,
        FolderMapper extends BaseMapper<F>,
        RelationMapper extends BaseMapper<Relation>> implements IFolderHandler<F> {
    private final FolderMapper folderMapper;

    private final RelationMapper relationMapper;

    public FolderHandlerImpl(FolderMapper folderMapper, RelationMapper relationMapper) {
        this.folderMapper = folderMapper;
        this.relationMapper = relationMapper;
    }

    public Condition getWhere(QueryFolderPojo query) {
        Condition condition = DSL.noCondition();
        if (StringUtils.isNotEmpty(query.getParentId())) {
            condition = condition.and(DSL.field("ancestor_id").eq(query.getParentId()));
        } else {
            condition = condition.and(DSL.field("ancestor_id").isNull());
        }
        if (query.getDepth() != null) {
            condition = condition.and(DSL.field("depth").eq(query.getDepth()));
        }
        if (StringUtils.isNotEmpty(query.getName())) {
            condition = condition.and(DSL.field("name").like("%" + query.getName() + "%"));
        }
        return DSL.field("id").in(relationMapper.getDslContext().select(DSL.field("descendant_id"))
                .from(relationMapper.getTable())
                .where(condition));
    }

    @Override
    public Future<F> get(String folderId) {
        return folderMapper.getById(folderId);
    }


    @Override
    public Future<Boolean> delete(String folderId) {
        return folderMapper.deleteById(folderId)
                .compose(ok -> {
                    return relationMapper
                            .delete(DSL.field("descendant_id")
                                            .eq(DSL.param("#{descendant_id}")).or(DSL.field("ancestor_id").eq(DSL.param("#{ancestor_id}"))),
                                    Map.of("descendant_id", folderId,
                                            "ancestor_id", folderId));
                }).compose(_ -> Future.succeededFuture(Boolean.TRUE));
    }

    @Override
    public Future<F> rename(String folderId, String name) {
        return folderMapper.getById(folderId)
                .compose(resource -> Tool.validateNodeName(folderMapper, getParentId(resource), name, UUID.fromString(folderId))
                        .compose(_ -> Future.succeededFuture(resource)))
                .compose(resource -> folderMapper.update(Map.of(DSL.field("name"), DSL.param("#{name}")),
                                DSL.field("id").eq(DSL.param("#{id}")),
                                Map.of("name", name,
                                        "id", folderId))
                        .compose(_ -> Future.succeededFuture(resource)))
                .compose(result -> {
                    setName(result, name);
                    return Future.succeededFuture(result);
                });
    }

    @Override
    public Future<F> create(CreateFolderPojo createFolderPojo) {
        String name = createFolderPojo.getName();
        UUID parentUuId = Tool.getParentUuId(createFolderPojo.getParentId());
        UUID nodeId = UUID.randomUUID();
        if (StringUtils.isEmpty(name)) {
            return Tool.getNodeRelation(relationMapper, parentUuId, nodeId, this::newRelation, this::getAncestorId, this::getDepth)
                    .compose(relationMapper::batch_save)
                    .compose(_ -> Tool.getAvailableNodeName(folderMapper, parentUuId, this::getName, this::getNamePrefix))
                    .compose(newName -> Future.succeededFuture(newFolder(nodeId, parentUuId, newName, createFolderPojo.getDesc())))
                    .compose(n -> folderMapper.save(n).compose(ok -> Future.succeededFuture(n)));
        } else {
            F f = newFolder(nodeId, parentUuId, name, createFolderPojo.getDesc());
            return Tool.validateNodeName(folderMapper, parentUuId, name, null)
                    .compose(ok -> Tool.getNodeRelation(relationMapper, parentUuId, nodeId, this::newRelation, this::getAncestorId, this::getDepth))
                    .compose(relationMapper::batch_save)
                    .compose(ok -> folderMapper.save(f))
                    .compose(ok -> Future.succeededFuture(f));
        }
    }

    @Override
    public Future<Boolean> move(String folderId, String targetFolderId) {
        return Tool.move(folderMapper, relationMapper, this::getParentId, this::getName, this::newRelation, this::getAncestorId, this::getDepth, folderId, folderId);
    }

    public void rename(RoutingContext context) {
        String id = context.pathParam("folderId");
        String name = context.body().asJsonObject().getString("name");
        rename(id, name)
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(Future::failedFuture);

    }

    public void delete(RoutingContext context) {
        String id = context.pathParam("folderId");
        if (Strings.CS.equals(id, ROOT_FOLDER_ID_STR)) {
            throw new ApiException(500, "跟目录不能删除");
        }
        delete(id).onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(Future::failedFuture);
    }

    public void move(RoutingContext context) {
        String id = context.pathParam("id");
        String folderId = context.pathParam("folderId");
        this.move(id, folderId)
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(Future::failedFuture);
    }

    public void get(RoutingContext context) {
        String id = context.pathParam("folderId");
        this.get(id).onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(Future::failedFuture);
    }

    @Override
    public void create(RoutingContext context) {
        String folderId = Tool.getParentId(context.pathParam("folderId"));
        CreateFolderPojo pojo = context.body().asPojo(CreateFolderPojo.class);
        pojo.setParentId(folderId);
        this.create(pojo).onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    protected abstract UUID getParentId(F resource);

    protected String getNamePrefix() {
        return "新建文件夹";
    }

    protected abstract void setName(F resource, String name);

    protected abstract String getName(F resource);

    protected abstract Relation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept);

    protected abstract F newFolder(UUID id, UUID parentId, String name, String desc);

    /**
     * 获取前辈id
     *
     * @param relation 关联关系
     * @return 前辈id
     */
    protected abstract UUID getAncestorId(Relation relation);

    protected abstract Integer getDepth(Relation relation);

}
