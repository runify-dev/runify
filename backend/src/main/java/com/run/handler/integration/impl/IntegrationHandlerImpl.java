package com.run.handler.integration.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Integration;
import com.run.dao.entity.IntegrationFolder;
import com.run.dao.entity.IntegrationPermission;
import com.run.dao.entity.IntegrationRelation;
import com.run.dao.mapper.IntegrationFolderMapper;
import com.run.dao.mapper.IntegrationMapper;
import com.run.dao.mapper.IntegrationPermissionMapper;
import com.run.dao.mapper.IntegrationRelationMapper;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.integration.IIntegrationHandler;
import com.run.handler.integration.IntegrationCredentialMask;
import com.run.handler.integration.pojo.IntegrationCreateVO;
import com.run.handler.integration.pojo.IntegrationEditPojo;
import com.run.integrations.impl.weixin.WeixinPollerManager;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class IntegrationHandlerImpl extends ResourceHandlerImpl<Integration, IntegrationFolder, IntegrationPermission, IntegrationRelation, IntegrationMapper, IntegrationFolderMapper, IntegrationPermissionMapper, IntegrationRelationMapper> implements IIntegrationHandler {

    private final WeixinPollerManager weixinPollerManager;

    @Inject
    public IntegrationHandlerImpl(IntegrationMapper integrationMapper, IntegrationFolderMapper integrationFolderMapper, IntegrationRelationMapper integrationRelationMapper, IntegrationPermissionMapper integrationPermissionMapper, CacheStore cacheStore, WeixinPollerManager weixinPollerManager) {
        super(integrationMapper, integrationFolderMapper, integrationRelationMapper, integrationPermissionMapper, cacheStore);
        this.weixinPollerManager = weixinPollerManager;
    }

    @Override
    public void get(RoutingContext context) {
        String id = context.pathParam("resourceId");
        resourceMapper.getResourceById(id)
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    @SneakyThrows
    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");

        resourceMapper.getById(resourceId)
                .compose(integration -> {
                    IntegrationEditPojo body = context.body().asPojo(IntegrationEditPojo.class);

                    JsonObject oldConfig = integration.decrypt();
                    Map<String, Object> masked = IntegrationCredentialMask.mask(oldConfig.getMap());
                    boolean eq = body.getConfig() != null && body.getConfig().getMap().equals(masked);

                    String oldEncryptedConfig = integration.getConfig();

                    CommonUtils.copyProperties(body, integration);

                    if (eq) {
                        integration.setConfig(oldEncryptedConfig);
                    } else {
                        integration.setConfig(integration.encrypt(body.getConfig() == null ? new JsonObject() : body.getConfig()));
                    }
                    integration.setUpdateTime(LocalDateTime.now());
                    return resourceMapper.update(integration).map(ok -> integration);
                })
                .onSuccess(integration -> {
                    // 运行时按启用状态管理 poller: edit 之前只写库不碰 poller, 导致改完要重启后端才生效。
                    // 个人微信(WEIXIN)是常驻 poller, 启用->start(内部会先 stop 再起, 解密拿 token), 停用->显式 stop。
                    if ("WEIXIN".equals(integration.getType())) {
                        if (Boolean.FALSE.equals(integration.getEnabled())) {
                            weixinPollerManager.stop(integration.getId().toString());
                        } else {
                            weixinPollerManager.start(integration);
                        }
                    }
                    context.end(Result.success(true).toBuffer());
                })
                .onFailure(context::fail);
    }

    @Override
    protected SimpleNodePojo resourceToSimpleNodePojo(Integration integration) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(integration, simpleNodePojo);
        simpleNodePojo.setType("integration");
        return simpleNodePojo;
    }

    @Override
    protected SimpleNodePojo folderToSimpleNodePojo(IntegrationFolder integrationFolder) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(integrationFolder, simpleNodePojo);
        simpleNodePojo.setType("folder");
        return simpleNodePojo;
    }

    @Override
    public Boolean resourceRead(RoutingContext context) {
        UserProfile userProfile = context.user().get("user");
        PermissionConstants.Permission permission = PermissionConstants.INTEGRATION_READ.getPermission();
        return userProfile.getPermissions().containsKey(permission.toString());
    }

    @Override
    protected IntegrationRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new IntegrationRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected UUID getParentId(Integration resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(Integration resource, String name) {
        resource.setName(name);
    }

    @Override
    protected UUID getAncestorId(IntegrationRelation integrationRelation) {
        return integrationRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(IntegrationRelation integrationRelation) {
        return integrationRelation.getDepth();
    }

    @Override
    protected String getName(Integration resource) {
        return resource.getName();
    }

    @Override
    protected UUID getTarget(IntegrationPermission permission) {
        return permission.getTarget();
    }

    @Override
    protected String getPermission(IntegrationPermission permission) {
        return permission.getPermission();
    }

    @Override
    protected String getNamePrefix() {
        return "新建集成";
    }

    @Override
    protected Integration newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context) {
        IntegrationCreateVO vo = context.body().asPojo(IntegrationCreateVO.class);
        LocalDateTime now = LocalDateTime.now();
        Integration integration = new Integration(resourceId, parentUuId, name, vo.getDesc(), "", vo.getType(),
                vo.getApplicationId(), "", vo.getEnabled() != null ? vo.getEnabled() : Boolean.TRUE,
                new JsonObject(), now, now);
        integration.setConfig(integration.encrypt(vo.getConfig() == null ? new JsonObject() : vo.getConfig()));
        return integration;
    }

    @Override
    protected IntegrationPermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new IntegrationPermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }
}
