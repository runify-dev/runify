package com.run.handler.datasource.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.datasources.DatasourceProviderConstants;
import com.run.datasources.DatasourceProviderInfo;
import com.run.datasources.DataSourceType;
import com.run.datasources.IDatasourceProvider;
import com.run.datasources.DataSourceManage;
import com.run.datasources.SqlProvider;
import com.run.dao.entity.DataSourceFolder;
import com.run.dao.entity.DataSourcePermission;
import com.run.dao.entity.DataSourceRelation;
import com.run.dao.entity.Datasource;
import com.run.dao.mapper.DataSourceFolderMapper;
import com.run.dao.mapper.DataSourcePermissionMapper;
import com.run.dao.mapper.DataSourceRelationMapper;
import com.run.dao.mapper.DatasourceMapper;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.datasource.IDataSourceHandler;
import com.run.handler.datasource.vo.CreateDataSourceVO;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/1  22:31}
 * {@code @Version 1.0}
 * {@code @注释: 数据源连接池处理器实现}
 */
public class DataSourceHandlerImpl extends ResourceHandlerImpl<Datasource, DataSourceFolder, DataSourcePermission, DataSourceRelation, DatasourceMapper, DataSourceFolderMapper, DataSourcePermissionMapper, DataSourceRelationMapper> implements IDataSourceHandler {

    @Inject
    public DataSourceHandlerImpl(DatasourceMapper datasourceMapper,
                                 DataSourceFolderMapper dataSourceFolderMapper,
                                 DataSourceRelationMapper dataSourceRelationMapper,
                                 DataSourcePermissionMapper dataSourcePermissionMapper,
                                 CacheStore cacheStore) {
        super(datasourceMapper, dataSourceFolderMapper, dataSourceRelationMapper, dataSourcePermissionMapper, cacheStore);
    }

    @Override
    public void get(RoutingContext context) {
        String id = context.pathParam("resourceId");
        resourceMapper.getById(id).compose(datasource -> {
                    if (datasource == null) {
                        return Future.failedFuture("数据源不存在");
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", datasource.getId());
                    result.put("name", datasource.getName());
                    result.put("desc", datasource.getDesc());
                    result.put("dataSourceType", datasource.getDataSourceType());
                    result.put("provider", datasource.getProvider());
                    Map<String, Object> meta = datasource.decrypt().getMap();
                    if (datasource.getProvider() != null) {
                        IDatasourceProvider provider = datasource.getProvider().getProvider();
                        meta = provider.getCredential().encryption(meta);
                    }
                    result.put("meta", meta);
                    result.put("parentId", datasource.getParentId());
                    result.put("createTime", datasource.getCreateTime());
                    result.put("updateTime", datasource.getUpdateTime());
                    return Future.succeededFuture(result);
                }).onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        CreateDataSourceVO vo = context.body().asPojo(CreateDataSourceVO.class);
        resourceMapper.getById(resourceId).compose(old -> {
            DatasourceProviderConstants providerEnum = DatasourceProviderConstants.valueOf(vo.getProvider());
            IDatasourceProvider provider = providerEnum.getProvider();
            JsonObject oldMeta = old.decrypt();
            Map<String, Object> encrypted = provider.getCredential().encryption(oldMeta.getMap());
            boolean metaUnchanged = vo.getMeta().getMap().equals(encrypted);
            Datasource datasource = new Datasource();
            datasource.setId(UUID.fromString(resourceId));
            datasource.setName(vo.getName());
            datasource.setDesc(vo.getDesc());
            datasource.setProvider(providerEnum);
            datasource.setMeta(metaUnchanged ? old.getMeta() : old.encrypt(vo.getMeta()));
            JsonObject finalMeta = metaUnchanged ? oldMeta : vo.getMeta();
            return provider.validate(datasource, Vertx.vertx()).compose(ok ->
                    resourceMapper.update(datasource).map(ds -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("id", datasource.getId());
                        result.put("name", datasource.getName());
                        result.put("desc", datasource.getDesc());
                        result.put("dataSourceType", datasource.getDataSourceType());
                        result.put("provider", datasource.getProvider());
                        result.put("meta", provider.getCredential().encryption(finalMeta.getMap()));
                        return result;
                    })
            );
        }).onSuccess(rs -> {
            context.end(Result.success(rs).toBuffer());
        }).onFailure(context::fail);
    }

    @Override
    public void getDataSourceTypes(RoutingContext context) {
        List<Map<String, Object>> list = Arrays.stream(DataSourceType.values())
                .map(type -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("code", type.getCode());
                    map.put("message", type.getMessage());
                    map.put("icon", type.getIcon());
                    return map;
                })
                .toList();
        context.end(Result.success(list).toBuffer());
    }

    @Override
    public void getProviders(RoutingContext context) {
        String type = context.pathParam("type");
        try {
            DataSourceType dataSourceType = DataSourceType.valueOf(type);
            List<DatasourceProviderInfo> list = Arrays.stream(DatasourceProviderConstants.values())
                    .map(p -> p.getProvider().info())
                    .filter(info -> info.getType() == dataSourceType)
                    .toList();
            context.end(Result.success(list).toBuffer());
        } catch (IllegalArgumentException e) {
            context.fail(400, new RuntimeException("不支持的数据源类型: " + type));
        }
    }

    @Override
    public void getFormDefinition(RoutingContext context) {
        String provider = context.pathParam("provider");
        try {
            DatasourceProviderConstants providerEnum = DatasourceProviderConstants.valueOf(provider);
            IDatasourceProvider poolProvider = providerEnum.getProvider();
            List<Map<String, Object>> formList = poolProvider.getFormList();
            context.end(Result.success(formList).toBuffer());
        } catch (IllegalArgumentException e) {
            context.fail(400, new RuntimeException("不支持的数据库类型: " + provider));
        }
    }

    @Override
    public void getTables(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        resourceMapper.getById(resourceId).compose(datasource -> {
            if (datasource == null) {
                return Future.failedFuture("数据源不存在");
            }
            IDatasourceProvider provider = datasource.getProvider().getProvider();
            if (!(provider instanceof SqlProvider sqlProvider)) {
                return Future.failedFuture("不支持的数据源类型");
            }
            Pool pool = DataSourceManage.getPool(datasource, Vertx.vertx());
            return sqlProvider.getTables(pool);
        }).onSuccess(tables -> context.end(Result.success(tables).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void getColumns(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        String tableName = context.pathParam("tableName");
        resourceMapper.getById(resourceId).compose(datasource -> {
            if (datasource == null) {
                return Future.failedFuture("数据源不存在");
            }
            IDatasourceProvider provider = datasource.getProvider().getProvider();
            if (!(provider instanceof SqlProvider sqlProvider)) {
                return Future.failedFuture("不支持的数据源类型");
            }
            Pool pool = DataSourceManage.getPool(datasource, Vertx.vertx());
            return sqlProvider.getColumns(pool, tableName);
        }).onSuccess(columns -> context.end(Result.success(columns).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    protected SimpleNodePojo resourceToSimpleNodePojo(Datasource pool) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(pool, simpleNodePojo);
        simpleNodePojo.setType("datasource");
        return simpleNodePojo;
    }

    @Override
    protected SimpleNodePojo folderToSimpleNodePojo(DataSourceFolder folder) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(folder, simpleNodePojo);
        simpleNodePojo.setType("folder");
        return simpleNodePojo;
    }

    @Override
    public Boolean resourceRead(RoutingContext context) {
        UserProfile userProfile = context.user().get("user");
        PermissionConstants.Permission permission = PermissionConstants.PROJECT_READ.getPermission();
        return userProfile.getPermissions().containsKey(permission.toString());
    }

    @Override
    protected DataSourceRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new DataSourceRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected UUID getParentId(Datasource resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(Datasource resource, String name) {
        resource.setName(name);
    }

    @Override
    protected UUID getAncestorId(DataSourceRelation relation) {
        return relation.getAncestorId();
    }

    @Override
    protected Integer getDepth(DataSourceRelation relation) {
        return relation.getDepth();
    }

    @Override
    protected String getName(Datasource resource) {
        return resource.getName();
    }

    @Override
    protected UUID getTarget(DataSourcePermission permission) {
        return permission.getTarget();
    }

    @Override
    protected String getPermission(DataSourcePermission permission) {
        return permission.getPermission();
    }

    @Override
    protected String getNamePrefix() {
        return "新建数据源";
    }

    @Override
    protected Datasource newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context) {
        CreateDataSourceVO vo = context.body().asPojo(CreateDataSourceVO.class);
        Datasource datasource = new Datasource();
        datasource.setId(resourceId);
        datasource.setParentId(parentUuId);
        datasource.setName(name);
        datasource.setDesc(vo.getDesc());
        datasource.setDataSourceType(DataSourceType.valueOf(vo.getDataSourceType()));
        datasource.setProvider(DatasourceProviderConstants.valueOf(vo.getProvider()));
        datasource.setMeta(datasource.encrypt(vo.getMeta()));
        datasource.setCreateTime(LocalDateTime.now());
        datasource.setUpdateTime(LocalDateTime.now());
        return datasource;
    }

    @Override
    protected DataSourcePermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new DataSourcePermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }
}
