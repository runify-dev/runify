package com.run.handler.common.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.constants.ResourcePermissionConstants;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.TreeUtil;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ApplicationPermission;
import com.run.dao.entity.Role;
import com.run.dao.entity.RolePermissionRelation;
import com.run.dao.entity.User;
import com.run.handler.common.IResourceHandler;
import com.run.handler.common.Tool;
import com.run.handler.common.pojo.QueryResourcePojo;
import com.run.handler.common.pojo.SimpleNodePermissionPojo;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.datasource.vo.CreateDataSourceVO;
import com.run.handler.tree.pojo.CreateSimpleNodePojo;
import com.run.sql.DSL;
import com.run.sql.condition.Condition;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  16:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public abstract class ResourceHandlerImpl<R extends BaseEntity<R>,
        F extends BaseEntity<F>,
        P extends BaseEntity<P>,
        Relation extends BaseEntity<Relation>,
        ResourceMapper extends BaseMapper<R>,
        FolderMapper extends BaseMapper<F>,
        PermissionMapper extends BaseMapper<P>,
        RelationMapper extends BaseMapper<Relation>> implements IResourceHandler
        <R> {
    protected final ResourceMapper resourceMapper;
    protected final RelationMapper relationMapper;
    protected final FolderMapper folderMapper;
    protected final PermissionMapper permissionMapper;
    protected final CacheStore cacheStore;

    public ResourceHandlerImpl(ResourceMapper resourceMapper,
                               FolderMapper folderMapper,
                               RelationMapper relationMapper,
                               PermissionMapper permissionMapper,
                               CacheStore cacheStore) {
        this.resourceMapper = resourceMapper;
        this.relationMapper = relationMapper;
        this.folderMapper = folderMapper;
        this.permissionMapper = permissionMapper;
        this.cacheStore = cacheStore;
    }

    public Condition getWhere(QueryResourcePojo query) {
        Condition condition = DSL.noCondition();
        if (StringUtils.isNotEmpty(query.getParentId())) {
            condition = condition.and(DSL.field("ancestor_id").eq(query.getParentId()));
        } else {
            condition = condition.and(DSL.field("ancestor_id").eq(new UUID(0, 0).toString()));
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

    public static <PermissionMapper extends BaseMapper<P>, P extends BaseEntity<P>,
            RelationMapper extends BaseMapper<Relation>, Relation extends BaseEntity<Relation>> Condition getWhereByPermission(PermissionMapper permissionMapper, RelationMapper relationMapper, QueryResourcePojo query, Boolean resourceRead) {
        Condition relationCondition = DSL.noCondition();
        Condition resourceCondition = DSL.noCondition();
        if (StringUtils.isNotEmpty(query.getParentId())) {
            relationCondition = relationCondition.and(DSL.field("ancestor_id")
                    .eq(DSL.param("folderId")));
        }
        if (query.getDepth() != null) {
            relationCondition = relationCondition.and(DSL.field("depth").eq(DSL.param("depth")));
        }
        if (StringUtils.isNotEmpty(query.getName())) {
            resourceCondition = resourceCondition.and(DSL.field("name").like(DSL.param("name", String.class)));
        }
        Condition baseCondition = DSL.field("id")
                .in(permissionMapper.getDslContext().select(DSL.field("target")).from(permissionMapper.getTable()).where(DSL.field("permission").in(resourceRead ? List.of("VIEW", "MANAGE", "ROLE") : List.of("VIEW", "MANAGE"))));

        if (relationCondition.isEmpty()) {
            baseCondition = baseCondition.and(DSL.field("id").in(relationMapper.getDslContext().select(DSL.field("descendant_id")).from(relationMapper.getTable()).where(relationCondition)));
        }
        if (resourceCondition.isEmpty()) {
            baseCondition = baseCondition.and(resourceCondition);
        }
        return baseCondition;
    }

    public Condition getWhereByPermission(QueryResourcePojo query, Boolean resourceRead) {
        return getWhereByPermission(permissionMapper, relationMapper, query, resourceRead);
    }

    @Override
    public Future<R> get(String resourceId) {
        return resourceMapper.getById(resourceId);
    }

    @Override
    public Future<List<R>> list(QueryResourcePojo query) {
        Condition where = getWhere(query);
        return resourceMapper.list(where);
    }

    @Override
    public Future<List<R>> listByPermission(QueryResourcePojo query, UUID userId, Boolean resourceRead) {
        Condition where = getWhereByPermission(query, resourceRead);
        return resourceMapper.list(where, Map.of(
                "folderId", Optional.ofNullable(query.getParentId()).orElse(""),
                "depth", Optional.ofNullable(query.getDepth()).orElse(-1),
                "name", Optional.ofNullable(query.getName()).orElse(""),
                "permissionView", "VIEW",
                "permissionManage", "MANAGE",
                "permissionNotAuth", "NOT_AUTH",
                "permissionRole", "ROLE",
                "userId", userId));
    }

    @Override
    public Future<List<SimpleNodePojo>> treeByPermission(QueryResourcePojo query, UUID userId, Boolean resourceRead) {
        Map<String, Object> params = Map.of(
                "folderId", Optional.ofNullable(query.getParentId()).orElse(""),
                "depth", Optional.ofNullable(query.getDepth()).orElse(-1),
                "name", Optional.ofNullable(query.getName()).orElse(""),
                "permissionView", "VIEW",
                "permissionManage", "MANAGE",
                "permissionNotAuth", "NOT_AUTH",
                "permissionRole", "ROLE",
                "userId", userId);
        Condition where = getWhereByPermission(query, resourceRead);
        return resourceMapper.list(where, params).compose(rs ->
                folderMapper.list(where, params).compose(r -> {
                    List<SimpleNodePojo> simpleNodePojoList = new ArrayList<>();
                    List<SimpleNodePojo> list2 = rs.stream().map(this::resourceToSimpleNodePojo).toList();
                    List<SimpleNodePojo> list1 = r.stream().map(this::folderToSimpleNodePojo).toList();
                    simpleNodePojoList.addAll(list1);
                    simpleNodePojoList.addAll(list2);
                    return Future.succeededFuture(simpleNodePojoList);
                })
        ).onFailure(e -> {
            System.out.println(e);
        });
    }

    @Override
    public Future<List<SimpleNodePojo>> tree(QueryResourcePojo query) {
        Map<String, Object> params = Map.of(
                "folderId", Optional.ofNullable(query.getParentId()).orElse(""),
                "depth", Optional.ofNullable(query.getDepth()).orElse(-1),
                "name", Optional.ofNullable(query.getName()).orElse(""));
        Condition where = getWhere(query);
        return Future.all(resourceMapper.list(where, params), folderMapper.list(where, params))
                .compose(compositeFuture -> {
                    List<R> rs = compositeFuture.resultAt(0);
                    List<F> r = compositeFuture.resultAt(1);
                    List<SimpleNodePojo> simpleNodePojoList = new ArrayList<>();
                    List<SimpleNodePojo> list2 = rs.stream().map(this::resourceToSimpleNodePojo).toList();
                    List<SimpleNodePojo> list1 = r.stream().map(this::folderToSimpleNodePojo).toList();
                    simpleNodePojoList.addAll(list1);
                    simpleNodePojoList.addAll(list2);
                    return Future.succeededFuture(simpleNodePojoList);
                }).onFailure(e -> {
                    e.printStackTrace();
                });
    }

    protected abstract SimpleNodePojo resourceToSimpleNodePojo(R r);

    protected abstract SimpleNodePojo folderToSimpleNodePojo(F f);

    @Override
    public Future<Boolean> delete(String resourceId) {
        return resourceMapper.deleteById(resourceId)
                .compose(_ -> relationMapper
                        .delete(DSL.field("descendant_id")
                                        .eq(DSL.param("descendant_id")).or(DSL.field("ancestor_id").eq(DSL.param("ancestor_id"))),
                                Map.of("descendant_id", resourceId,
                                        "ancestor_id", resourceId)))
                .compose(_ -> Future.succeededFuture(Boolean.TRUE));
    }

    @Override
    public Future<R> rename(String resourceId, String name) {
        return resourceMapper.getById(resourceId)
                .compose(resource -> Tool.validateNodeName(resourceMapper, getParentId(resource), name, UUID.fromString(resourceId))
                        .compose(_ -> Future.succeededFuture(resource)))
                .compose(resource -> resourceMapper.update(Map.of(DSL.field("name"), DSL.param("name")),
                                DSL.field("id").eq(DSL.param("id")),
                                Map.of("name", name,
                                        "id", resourceId))
                        .compose(_ -> Future.succeededFuture(resource)))
                .compose(result -> {
                    setName(result, name);
                    return Future.succeededFuture(result);
                });
    }

    @Override
    public void create(RoutingContext context) {
        io.vertx.ext.auth.User user = context.user();
        UserProfile userProfile = (UserProfile) user.get("user");
        List<Role> roles = userProfile.getRoles();
        UUID userId = userProfile.getId();
        boolean isAdmin = roles.stream().anyMatch(r -> r.getId().equals(PermissionConstants.Role.ADMIN.name()));
        UUID parentUuId = TreeUtil.getParentUuId(context.pathParam("folderId"));
        CreateSimpleNodePojo createSimpleResourcePojo = context.body().asPojo(CreateSimpleNodePojo.class);
        String name = createSimpleResourcePojo.getName();
        Future<R> future;
        UUID nodeId = UUID.randomUUID();
        if (StringUtils.isEmpty(name)) {
            future = Tool.getNodeRelation(relationMapper, parentUuId, nodeId, this::newRelation, this::getAncestorId, this::getDepth)
                    .compose(relationMapper::batch_save)
                    .compose(_ -> Tool.getAvailableNodeName(resourceMapper, parentUuId, this::getName, this::getNamePrefix))
                    .compose(newName -> Future.succeededFuture(newResource(nodeId, parentUuId, newName, context)))
                    .compose(n -> resourceMapper.save(n).compose(ok -> Future.succeededFuture(n)));
        } else {
            R resource = newResource(nodeId, parentUuId, name, context);
            future = Tool.validName(resourceMapper, parentUuId, name)
                    .compose(_ -> Tool.getNodeRelation(relationMapper, parentUuId, nodeId, this::newRelation, this::getAncestorId, this::getDepth))
                    .compose(relationMapper::batch_save)
                    .compose(ok -> resourceMapper.save(resource))
                    .compose(ok -> Future.succeededFuture(resource));
        }
        if (!isAdmin) {
            future = future.compose(source -> {
                P p = newPermission(UUID.randomUUID(), userId, nodeId, PermissionConstants.ResourcePermissionGroup.MANAGE.name());
                return permissionMapper.save(p).compose(_ -> Future.fromCompletionStage(cacheStore.delete("permissions:" + userId)).compose(_ -> Future.succeededFuture(source)));
            });
        }
        future
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public Future<Boolean> move(String resourceId, String folderId) {
        return Tool.move(resourceMapper, relationMapper, this::getParentId, this::getName, this::newRelation, this::getAncestorId, this::getDepth, resourceId, folderId);
    }

    public void list(RoutingContext context) {
        String folderId = Tool.getParentId(context.pathParam("folderId"));
        MultiMap entries = context.queryParams();
        User user = context.user().get("user");
        String role = user.getRole();
        QueryResourcePojo query = new QueryResourcePojo(folderId, entries.get("name"), null);
        (role.equals("ADMIN") ? list(query) : listByPermission(query, user.getId(), resourceRead(context)))
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);

    }

    public abstract Boolean resourceRead(RoutingContext context);

    public void tree(RoutingContext context) {
        String folderId = Tool.getParentId(context.pathParam("folderId"));
        MultiMap entries = context.queryParams();
        User user = context.user().get("user");
        String role = user.getRole();
        QueryResourcePojo query = new QueryResourcePojo(folderId, entries.get("name"), null);
        (role.equals("ADMIN") ? tree(query) : treeByPermission(query, user.getId(), resourceRead(context)))
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    public void rename(RoutingContext context) {
        String id = context.pathParam("resourceId");
        String name = context.body().asJsonObject().getString("name");
        rename(id, name)
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(Future::failedFuture);

    }

    public void delete(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        delete(resourceId).onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
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
        String id = context.pathParam("resourceId");
        this.get(id).onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(Future::failedFuture);
    }

    @Override
    public void listResourcePermission(RoutingContext context) {
        String folderId = Tool.getParentId(context.pathParam("folderId"));
        String userId = context.pathParam("userId");
        MultiMap entries = context.queryParams();
        User user = context.user().get("user");
        String role = user.getRole();
        QueryResourcePojo query = new QueryResourcePojo(folderId, entries.get("name"), null);
        (role.equals("ADMIN") ? tree(query) : treeByPermission(query, user.getId(), resourceRead(context)))
                .compose(rs -> permissionMapper
                        .list(DSL.field("user_id").eq(DSL.param("userId")), Map.of("userId", userId))
                        .compose(ps -> Future.succeededFuture(toSimpleNodePermissionPojo(rs, ps))))
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(Future::failedFuture);
    }

    @Override
    public void authResourcePermission(RoutingContext context) {
        String userId = context.pathParam("userId");
        String resourceId = context.pathParam("resourceId");
        String permission = context.pathParam("permission");
        P p = newPermission(UUID.randomUUID(), UUID.fromString(userId), UUID.fromString(resourceId), permission);
        permissionMapper.delete(DSL.field("user_id").eq(DSL.param("userId"))
                                .and(DSL.field("target").eq(DSL.param("target"))),
                        Map.of("userId", userId, "permission", permission, "target", resourceId))
                .compose(_ -> permissionMapper.save(p))
                .compose(_ -> Future.fromCompletionStage(cacheStore.delete("permissions:" + userId)))
                .onSuccess(rs -> context.end(Result.success(p).toBuffer()))
                .onFailure(Future::failedFuture);
    }

    protected abstract Relation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept);

    protected abstract UUID getParentId(R resource);

    protected abstract void setName(R resource, String name);

    /**
     * 获取前辈id
     *
     * @param relation 关联关系
     * @return 前辈id
     */
    protected abstract UUID getAncestorId(Relation relation);

    protected abstract Integer getDepth(Relation relation);

    protected abstract String getName(R resource);

    protected abstract UUID getTarget(P permission);

    protected abstract String getPermission(P permission);

    protected List<SimpleNodePermissionPojo> toSimpleNodePermissionPojo(List<SimpleNodePojo> simpleNodePojoList, List<P> permissionList) {
        return simpleNodePojoList.stream().map(item -> {
            SimpleNodePermissionPojo simpleNodePermissionPojo = new SimpleNodePermissionPojo();
            CommonUtils.copyProperties(item, simpleNodePermissionPojo);
            Optional<P> first = permissionList.stream().filter(p -> item.getId().equals(getTarget(p))).findFirst();
            simpleNodePermissionPojo.setPermission(first.map(this::getPermission).orElseGet(() -> item.getParentId() == null ? ResourcePermissionConstants.NO_AUTH.name() : ResourcePermissionConstants.INHERIT.name()));
            return simpleNodePermissionPojo;
        }).toList();
    }

    protected abstract String getNamePrefix();

    protected abstract R newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context);

    protected abstract P newPermission(UUID id, UUID userId, UUID target, String permission);
}
