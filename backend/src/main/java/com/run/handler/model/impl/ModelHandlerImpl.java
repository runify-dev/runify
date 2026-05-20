package com.run.handler.model.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Model;
import com.run.dao.entity.ModelFolder;
import com.run.dao.entity.ModelPermission;
import com.run.dao.entity.ModelRelation;
import com.run.dao.mapper.ModelFolderMapper;
import com.run.dao.mapper.ModelMapper;
import com.run.dao.mapper.ModelPermissionMapper;
import com.run.dao.mapper.ModelRelationMapper;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.model.IModelHandler;
import com.run.handler.model.pojo.ModelEditPojo;
import com.run.models.IProvider;
import com.run.models.ModelInfo;
import com.run.models.ModelProvideConstants;
import com.run.models.ProvideInfo;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/5  22:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ModelHandlerImpl extends ResourceHandlerImpl<Model, ModelFolder, ModelPermission, ModelRelation, ModelMapper, ModelFolderMapper, ModelPermissionMapper, ModelRelationMapper> implements IModelHandler {

    @Inject
    public ModelHandlerImpl(ModelMapper modelMapper, ModelFolderMapper modelFolderMapper, ModelRelationMapper modelRelationMapper, ModelPermissionMapper modelPermissionMapper, CacheStore cacheStore) {
        super(modelMapper, modelFolderMapper, modelRelationMapper, modelPermissionMapper, cacheStore);
    }


    @Override
    public void getProvider(RoutingContext context) {
        List<ProvideInfo> list = Arrays.stream(ModelProvideConstants.values())
                .map(p -> p.getProvider().info()).toList();
        context.end(Result.success(list).toBuffer());
    }

    @Override
    public void getModelList(RoutingContext context) {
        String provider = context.pathParam("provider");
        List<Map<String, Object>> modelList = ModelProvideConstants.valueOf(provider).getProvider()
                .getModelInfoManage().getModelList()
                .stream().map(ModelInfo::toMap).toList();
        context.end(Result.success(modelList).toBuffer());
    }

    @Override
    public void listModelType(RoutingContext context) {
        String provider = context.pathParam("provider");
        List<HashMap<String, Object>> list = ModelProvideConstants.valueOf(provider).getProvider()
                .getModelInfoManage().getModelList().stream().map(ModelInfo::getModelType).distinct().map(item -> {
                    HashMap<String, Object> r = new HashMap<>();
                    r.put("code", item.getCode());
                    r.put("icon", item.getIcon());
                    r.put("message", item.getMessage());
                    return r;
                }).toList();
        context.end(Result.success(list).toBuffer());
    }

    @Override
    public void get(RoutingContext context) {
        String id = context.pathParam("resourceId");
        resourceMapper.getResourceById(id)
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(Future::failedFuture);
    }

    @SneakyThrows
    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");

        resourceMapper.getById(resourceId)
                .compose(model -> {
                    ModelEditPojo body = context.body().asPojo(ModelEditPojo.class);

                    JsonObject oldCredential = model.decrypt();

                    ModelInfo modelInfo = ModelProvideConstants
                            .valueOf(Objects.requireNonNullElse(body.getProvider(), model.getProvider()))
                            .getProvider()
                            .getModelInfo(
                                    Objects.requireNonNullElse(body.getModelType(), model.getModelType()),
                                    Objects.requireNonNullElse(body.getModelName(), model.getModelName())
                            );

                    Map<String, Object> encryption = modelInfo.getCredential().encryption(oldCredential.getMap());

                    boolean eq = body.getCredential().getMap().equals(encryption);

                    if (eq) {
                        body.setCredential(model.decrypt());
                    }

                    String oldEncryptedCredential = model.getCredential();

                    CommonUtils.copyProperties(body, model);

                    if (!eq) {
                        model.setCredential(model.encrypt(body.getCredential()));
                    } else {
                        model.setCredential(oldEncryptedCredential);
                    }

                    IProvider provider = ModelProvideConstants.valueOf(model.getProvider()).getProvider();

                    return validateInWorker(
                            context,
                            provider,
                            model.getModelType(),
                            model.getModelName(),
                            body.getCredential().getMap()
                    ).map(model);
                })
                .compose(resourceMapper::update)
                .onSuccess(ok -> context.end(Result.success(true).toBuffer()))
                .onFailure(context::fail);
    }

    private Future<Void> validateInWorker(
            RoutingContext context,
            IProvider provider,
            String modelType,
            String modelName,
            Map<String, Object> credential
    ) {
        return context.vertx().executeBlocking(() -> {
            provider.validate(modelType, modelName, credential, Map.of());
            return null;
        }, false);
    }

    @Override
    protected SimpleNodePojo resourceToSimpleNodePojo(Model model) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(model, simpleNodePojo);
        simpleNodePojo.setType("model");
        return simpleNodePojo;
    }

    @Override
    protected SimpleNodePojo folderToSimpleNodePojo(ModelFolder modelFolder) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(modelFolder, simpleNodePojo);
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
    protected ModelRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ModelRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected UUID getParentId(Model resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(Model resource, String name) {
        resource.setName(name);
    }

    @Override
    protected UUID getAncestorId(ModelRelation modelRelation) {
        return modelRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(ModelRelation modelRelation) {
        return modelRelation.getDepth();
    }

    @Override
    protected String getName(Model resource) {
        return resource.getName();
    }

    @Override
    protected UUID getTarget(ModelPermission permission) {
        return permission.getTarget();
    }

    @Override
    protected String getPermission(ModelPermission permission) {
        return permission.getPermission();
    }

    @Override
    protected String getNamePrefix() {
        return "新建模型";
    }

    @Override
    protected Model newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context) {
        return new Model(resourceId, parentUuId, name, "", "", "openai_provider", "LLM", "", "", new JsonArray(), new JsonObject(), false, false, LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected ModelPermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new ModelPermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }
}
