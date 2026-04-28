package com.run.handler.project.impl;

import com.run.common.constants.ProcessorProtocolConstants;
import com.run.common.exception.ApiException;
import com.run.common.project.ProjectManage;
import com.run.common.project.executor.ProcessorExecutor;
import com.run.common.query.Query;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Processor;
import com.run.dao.entity.Project;
import com.run.dao.mapper.ProcessorMapper;
import com.run.dao.mapper.ProjectMapper;
import com.run.handler.project.IProcessorHandler;
import com.run.handler.project.dto.ProcessorDto;
import com.run.handler.project.vo.CreateProcessorVO;
import com.run.handler.project.vo.EditProcessorVO;
import com.run.handler.project.vo.QueryProcessorVO;
import com.run.sql.condition.Condition;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.run.sql.DSL.field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/21  18:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProcessorHandlerImpl implements IProcessorHandler {
    private final ProcessorMapper processorMapper;
    private final ProjectMapper projectMapper;

    @Inject
    public ProcessorHandlerImpl(ProcessorMapper processorMapper, ProjectMapper projectMapper) {
        this.processorMapper = processorMapper;
        this.projectMapper = projectMapper;
    }

    @Override
    public void create(RoutingContext context) {
        String projectId = context.pathParam("projectId");
        CreateProcessorVO createProcessorVO = context.body().asPojo(CreateProcessorVO.class);
        Processor processor = new Processor(
                UUID.randomUUID(),
                UUID.fromString(projectId),
                createProcessorVO.getName(), createProcessorVO.getDesc(),
                ProcessorProtocolConstants.valueOf(createProcessorVO.getProtocol()),
                Boolean.FALSE,
                new JsonObject(), new JsonObject(Map.of()), LocalDateTime.now(), LocalDateTime.now());
        this.processorMapper.save(processor).onSuccess(ok -> {
            context.end(Result.success(processor).toBuffer());
        }).onFailure(context::fail);

    }

    public Condition getCondition(QueryProcessorVO query) {
        Condition condition = field(Processor::getProjectId).eq(query.getProjectId());
        if (StringUtils.isNotEmpty(query.getName())) {
            condition = condition.and(field(Processor::getName).like(query.getName()));
        }
        if (StringUtils.isNotEmpty(query.getDesc())) {
            condition = condition.and(field(Processor::getDesc).like(query.getDesc()));
        }
        if (StringUtils.isNotEmpty(query.getProtocol())) {
            condition = condition.and(field(Processor::getProtocol).eq(query.getProtocol()));
        }
        return condition;
    }

    @Override
    public void page(RoutingContext context) {
        String projectId = context.pathParam("projectId");
        QueryProcessorVO query = Query.format(QueryProcessorVO.class, context);
        Condition condition = getCondition(query);
        processorMapper.page(condition, query.getCurrentPage(), query.getPageSize(), new HashMap<>() {{
            put("name", "%" + query.getName() + "%");
            put("desc", "%" + query.getDesc() + "%");
            put("protocol", query.getProtocol());
            put("projectId", projectId);
        }}).onSuccess(ok -> {
            context.end(Result.success(ok).toBuffer());
        }).onFailure(context::fail);
    }

    @Override
    public void get(RoutingContext context) {
        String processorId = context.pathParam("processorId");
        processorMapper.getById(processorId)
                .onSuccess(ok -> {
                    ProcessorDto processorDto = new ProcessorDto();
                    CommonUtils.copyProperties(ok, processorDto);
                    processorDto.setIsDeploy(ProjectManage.isDeploy(ok.getProjectId(), ok.getId()));
                    context.end(Result.success(processorDto).toBuffer());
                })
                .onFailure(context::fail);
    }

    @Override
    public void edit(RoutingContext context) {
        String processorId = context.pathParam("processorId");
        EditProcessorVO editProcessorVO = context.body().asPojo(EditProcessorVO.class);
        processorMapper.getById(processorId).compose(processor -> {
                    if (processor == null) {
                        return Future.failedFuture(new ApiException(500, "不存在的处理器ID"));
                    }
                    if (StringUtils.isNotEmpty(editProcessorVO.getName())) {
                        processor.setName(editProcessorVO.getName());
                    }
                    if (StringUtils.isNotEmpty(editProcessorVO.getDesc())) {
                        processor.setName(editProcessorVO.getDesc());
                    }
                    if (editProcessorVO.getWorkflow() != null) {
                        processor.setWorkflow(editProcessorVO.getWorkflow());
                    }
                    if (editProcessorVO.getMeta() != null) {
                        processor.setMeta(editProcessorVO.getMeta());
                    }
                    return processorMapper.update(processor).compose(ok -> Future.succeededFuture(processor));
                }).onSuccess(processor -> context.end(Result.success(processor).toBuffer()))
                .onFailure(context::fail);

    }

    @Override
    public void deploy(RoutingContext context) {
        String processorId = context.pathParam("processorId");
        String projectId = context.pathParam("projectId");
        Future.all(projectMapper.getById(projectId), processorMapper.getById(processorId)).compose((compositeFuture) -> {
                    Project project = compositeFuture.resultAt(0);
                    Processor processor = compositeFuture.resultAt(1);
                    ProcessorExecutor processorExecutor = ProjectManage.generateProcessorExecutor(project, processor);
                    processorExecutor.deploy();
                    ProcessorDto processorDto = new ProcessorDto();
                    CommonUtils.copyProperties(processor, processorDto);
                    processorDto.setIsDeploy(ProjectManage.isDeploy(processor.getProjectId(), processor.getId()));
                    return Future.succeededFuture(processorDto);
                }).onSuccess(processor -> context.end(Result.success(processor).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void undeploy(RoutingContext context) {
        String processorId = context.pathParam("processorId");
        processorMapper.getById(processorId).compose(processor -> {
                    ProjectManage.unDeploy(processor.getProjectId(), processor.getId());
                    ProcessorDto processorDto = new ProcessorDto();
                    CommonUtils.copyProperties(processor, processorDto);
                    processorDto.setIsDeploy(ProjectManage.isDeploy(processor.getProjectId(), processor.getId()));
                    return Future.succeededFuture(processorDto);
                }).onSuccess(processor -> context.end(Result.success(processor).toBuffer()))
                .onFailure(context::fail);
    }
}
