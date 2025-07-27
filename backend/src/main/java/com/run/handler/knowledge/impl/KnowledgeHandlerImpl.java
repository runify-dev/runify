package com.run.handler.knowledge.impl;


import com.run.common.result.Result;
import com.run.dao.entity.Knowledge;
import com.run.dao.mapper.KnowledgeMapper;
import com.run.handler.knowledge.IKnowledgeHandler;
import com.run.handler.knowledge.pojo.EditKnowledge;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/14  22:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class KnowledgeHandlerImpl implements IKnowledgeHandler {
    private final KnowledgeMapper knowledgeMapper;

    @Inject
    public KnowledgeHandlerImpl(KnowledgeMapper knowledgeMapper) {
        this.knowledgeMapper = knowledgeMapper;
    }

    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        EditKnowledge editKnowledge = context.body().asPojo(EditKnowledge.class);
        Knowledge knowledge = new Knowledge();
        knowledge.setId(UUID.fromString(resourceId));
        knowledge.setContent(editKnowledge.getContent());
        if (StringUtils.isNotEmpty(editKnowledge.getContent())) {
            knowledge.setExcerpt(editKnowledge.getContent().substring(0, Math.min(editKnowledge.getContent().length(), 64)));
        }
        knowledge.setName(editKnowledge.getName());
        knowledgeMapper.update(knowledge).
                onSuccess(ok -> {
                    context.end(Result.success(knowledge).toBuffer());
                }).onFailure(context::fail);
    }


}
