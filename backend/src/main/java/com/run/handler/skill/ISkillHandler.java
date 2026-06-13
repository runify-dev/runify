package com.run.handler.skill;

import com.run.dao.entity.Skill;
import com.run.handler.common.IResourceHandler;
import io.vertx.ext.web.RoutingContext;

public interface ISkillHandler extends IResourceHandler<Skill> {

    void edit(RoutingContext context);

    void exportSkill(RoutingContext context);

    void importSkill(RoutingContext context);

    void installFromStore(RoutingContext context);

    void upgradeFromStore(RoutingContext context);
}
