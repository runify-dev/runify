package com.run.handler.model.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.entity.Model;
import com.run.dao.entity.ModelRelation;
import com.run.dao.mapper.ModelMapper;
import com.run.dao.mapper.ModelRelationMapper;
import com.run.handler.common.impl.TreeHandler;
import com.run.handler.model.IModelHandler;
import com.run.handler.model.pojo.ModelEditPojo;
import com.run.models.IProvider;
import com.run.models.ModelInfo;
import com.run.models.ModelProvideConstants;
import com.run.models.ProvideInfo;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
public class ModelHandlerImpl extends TreeHandler<Model, ModelRelation, ModelMapper, ModelRelationMapper> implements IModelHandler {
    protected ModelMapper modelMapper;
    private final Vertx vertx;

    @Inject
    public ModelHandlerImpl(ModelMapper modelMapper, ModelRelationMapper modelRelationMapper, Vertx vertx) {
        super(modelMapper, modelRelationMapper);
        this.modelMapper = modelMapper;
        this.vertx = vertx;
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

    @SneakyThrows
    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        modelMapper.getById(resourceId)
                .compose(model -> {
                    return vertx.executeBlocking(() -> {
                        ModelEditPojo body = context.body().asPojo(ModelEditPojo.class);
                        JsonObject oldCredential = model.decrypt();

                        ModelInfo modelInfo = ModelProvideConstants.valueOf(Objects.requireNonNullElse(body.getProvider(), model.getProvider())).getProvider()
                                .getModelInfo(Objects.requireNonNullElse(body.getModelType(), model.getModelType()),
                                        Objects.requireNonNullElse(body.getModelName(), model.getModelName()));
                        Map<String, Object> encryption = modelInfo.getCredential().encryption(oldCredential.getMap());
                        boolean eq = body.getCredential().getMap().equals(encryption);
                        if (eq) {
                            body.setCredential(model.decrypt());
                        }
                        String credential = model.getCredential();
                        CommonUtils.copyProperties(body, model);
                        if (!eq) {
                            model.setCredential(model.encrypt(body.getCredential()));
                        } else {
                            model.setCredential(credential);
                        }
                        IProvider provider = ModelProvideConstants.valueOf(model.getProvider()).getProvider();
                        provider.validate(model.getModelType(), model.getModelName(), body.getCredential().getMap(), Map.of());

                        return model;
                    });
                }).compose(modelMapper::update)
                .onSuccess(ok -> {
                    context.end(Result.success(true).toBuffer());
                })
                .onFailure(context::fail);
    }

    @Override
    protected String getNodeName(Model model) {
        return model.getName();
    }

    @Override
    protected UUID getNodeId(Model model) {
        return model.getId();
    }

    @Override
    protected ModelRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ModelRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected Model newNode(UUID id, UUID parentId, String type, String name) {
        return new Model(id, parentId, type, name, "", "openai_provider", "LLM", "", "", new JsonArray(), new JsonObject(), false, false, LocalDateTime.now(), LocalDateTime.now());
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
    protected Map<String, String> getNamePrefixMap() {
        return Map.of("model", "新建模型", "folder", "新建文件夹");
    }

    public void get(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        modelMapper.getById(resourceId)
                .compose(model -> {
                    if (model == null) {
                        return Future.succeededFuture(null);
                    }
                    if ("model".equals(model.getType())) {
                        String credential = model.getCredential();
                        String decrypt = RSAUtil.decrypt(credential);
                        JsonObject convert = JacksonUtils.convert(decrypt, JsonObject.class);
                        Map<String, Object> result = JacksonUtils.convert(model, new TypeReference<Map<String, Object>>() {
                        });
                        result.put("credential", convert);
                        return Future.succeededFuture(result);
                    }
                    return Future.succeededFuture(model);
                })
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(context::fail);
    }
}
