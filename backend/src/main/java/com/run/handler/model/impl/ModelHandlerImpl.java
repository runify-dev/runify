package com.run.handler.model.impl;

import com.run.common.result.Result;
import com.run.dao.mapper.ModelMapper;
import com.run.handler.model.IModelHandler;
import com.run.models.ModelInfo;
import com.run.models.ModelProvideConstants;
import com.run.models.ModelType;
import com.run.models.ProvideInfo;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/5  22:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ModelHandlerImpl implements IModelHandler {
    protected ModelMapper modelMapper;

    @Inject
    public ModelHandlerImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
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
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
    }
}
