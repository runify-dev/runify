package com.run.handler.tool.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Tool;
import com.run.dao.entity.ToolFolder;
import com.run.dao.entity.ToolPermission;
import com.run.dao.entity.ToolRelation;
import com.run.dao.mapper.ToolFolderMapper;
import com.run.dao.mapper.ToolMapper;
import com.run.dao.mapper.ToolPermissionMapper;
import com.run.dao.mapper.ToolRelationMapper;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.tool.IToolHandler;
import com.run.handler.tool.dto.ToolNodeDTO;
import com.run.handler.tool.pojo.EditToolPojo;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

public class ToolHandlerImpl extends ResourceHandlerImpl<Tool, ToolFolder, ToolPermission, ToolRelation, ToolMapper, ToolFolderMapper, ToolPermissionMapper, ToolRelationMapper> implements IToolHandler {

    private final ToolMapper toolMapper;

    @Inject
    public ToolHandlerImpl(ToolMapper toolMapper,
                           ToolFolderMapper toolFolderMapper,
                           ToolRelationMapper toolRelationMapper,
                           ToolPermissionMapper toolPermissionMapper,
                           CacheStore cacheStore) {
        super(toolMapper, toolFolderMapper, toolRelationMapper, toolPermissionMapper, cacheStore);
        this.toolMapper = toolMapper;
    }

    /**
     * secret 字段判定：configSchema 中标记 secret=true 或 type=PasswordInput
     */
    private boolean isSecretField(JsonObject field) {
        if (field == null) return false;
        return Boolean.TRUE.equals(field.getBoolean("secret"))
                || "PasswordInput".equals(field.getString("type"));
    }

    /**
     * 对 secret 配置值脱敏
     */
    private JsonObject maskConfig(JsonObject config, JsonArray configSchema) {
        if (config == null || config.isEmpty() || configSchema == null) {
            return config;
        }
        JsonObject masked = config.copy();
        for (int i = 0; i < configSchema.size(); i++) {
            JsonObject field = configSchema.getJsonObject(i);
            if (isSecretField(field)) {
                String key = field.getString("field");
                if (key != null && masked.getValue(key) != null) {
                    masked.put(key, CommonUtils.encryption(String.valueOf(masked.getValue(key))));
                }
            }
        }
        return masked;
    }

    /**
     * 合并配置值：secret 字段若提交值等于脱敏值则保留原值，否则用新值
     */
    private JsonObject mergeConfig(JsonObject original, JsonObject submitted, JsonArray configSchema) {
        if (configSchema == null) return submitted;
        JsonObject merged = submitted.copy();
        for (int i = 0; i < configSchema.size(); i++) {
            JsonObject field = configSchema.getJsonObject(i);
            if (isSecretField(field)) {
                String key = field.getString("field");
                if (key == null || !submitted.containsKey(key)) continue;
                Object submittedVal = submitted.getValue(key);
                Object originalVal = original.getValue(key);
                if (originalVal != null
                        && CommonUtils.encryption(String.valueOf(originalVal)).equals(String.valueOf(submittedVal))) {
                    merged.put(key, originalVal);
                }
            }
        }
        return merged;
    }

    private JsonObject toResult(Tool tool) {
        JsonObject result = JsonObject.mapFrom(tool);
        result.put("config", maskConfig(tool.decryptConfig(), tool.getConfigSchema()));
        return result;
    }

    @Override
    public void get(RoutingContext context) {
        String id = context.pathParam("resourceId");
        this.get(id)
                .onSuccess(tool -> context.end(Result.success(toResult(tool)).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        EditToolPojo pojo = context.body().asPojo(EditToolPojo.class);
        toolMapper.getById(resourceId).compose(tool -> {
                    if (StringUtils.isNotEmpty(pojo.getName())) tool.setName(pojo.getName());
                    if (pojo.getLabel() != null) tool.setLabel(pojo.getLabel());
                    if (pojo.getDescription() != null) tool.setDescription(pojo.getDescription());
                    if (StringUtils.isNotEmpty(pojo.getIcon())) tool.setIcon(pojo.getIcon());
                    if (StringUtils.isNotEmpty(pojo.getRuntime())) tool.setRuntime(pojo.getRuntime());
                    if (pojo.getInputSchema() != null) tool.setInputSchema(pojo.getInputSchema());
                    if (pojo.getOutputSchema() != null) tool.setOutputSchema(pojo.getOutputSchema());
                    if (pojo.getConfigSchema() != null) tool.setConfigSchema(pojo.getConfigSchema());
                    if (pojo.getBody() != null) tool.setBody(pojo.getBody());
                    if (pojo.getConfig() != null) {
                        JsonArray schema = pojo.getConfigSchema() != null ? pojo.getConfigSchema() : tool.getConfigSchema();
                        JsonObject merged = mergeConfig(tool.decryptConfig(), pojo.getConfig(), schema);
                        tool.setConfig(tool.encryptConfig(merged));
                    }
                    tool.setUpdateTime(LocalDateTime.now());
                    return toolMapper.update(tool);
                }).compose(_ -> toolMapper.getById(resourceId))
                .onSuccess(tool -> context.end(Result.success(toResult(tool)).toBuffer()))
                .onFailure(context::fail);
    }

    // ============ 资源处理器抽象方法 ============

    @Override
    protected SimpleNodePojo resourceToSimpleNodePojo(Tool tool) {
        ToolNodeDTO dto = new ToolNodeDTO();
        dto.setId(tool.getId());
        dto.setParentId(tool.getParentId());
        dto.setName(tool.getName());
        dto.setIcon(tool.getIcon());
        dto.setDesc(tool.getDescription());
        dto.setLabel(tool.getLabel());
        dto.setRuntime(tool.getRuntime());
        dto.setType("tool");
        return dto;
    }

    @Override
    protected SimpleNodePojo folderToSimpleNodePojo(ToolFolder toolFolder) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(toolFolder, simpleNodePojo);
        simpleNodePojo.setType("folder");
        return simpleNodePojo;
    }

    @Override
    public Boolean resourceRead(RoutingContext context) {
        UserProfile userProfile = context.user().get("user");
        PermissionConstants.Permission permission = PermissionConstants.TOOL_READ.getPermission();
        return userProfile.getPermissions().containsKey(permission.toString());
    }

    @Override
    protected ToolRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ToolRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected UUID getParentId(Tool resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(Tool resource, String name) {
        resource.setName(name);
    }

    @Override
    protected UUID getAncestorId(ToolRelation toolRelation) {
        return toolRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(ToolRelation toolRelation) {
        return toolRelation.getDepth();
    }

    @Override
    protected String getName(Tool resource) {
        return resource.getName();
    }

    @Override
    protected UUID getTarget(ToolPermission permission) {
        return permission.getTarget();
    }

    @Override
    protected String getPermission(ToolPermission permission) {
        return permission.getPermission();
    }

    @Override
    protected String getNamePrefix() {
        return "新建工具";
    }

    @Override
    protected Tool newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context) {
        String runtime = "JS";
        try {
            JsonObject body = context.body() != null ? context.body().asJsonObject() : null;
            if (body != null && StringUtils.isNotEmpty(body.getString("runtime"))) {
                runtime = body.getString("runtime");
            }
        } catch (Exception ignored) {
        }
        LocalDateTime now = LocalDateTime.now();
        return new Tool(resourceId, parentUuId, name, name, "", "", runtime,
                new JsonArray(), new JsonArray(), new JsonArray(), null, new JsonObject(), now, now);
    }

    @Override
    protected ToolPermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new ToolPermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }
}
