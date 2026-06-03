package com.run.handler.skill;

import com.run.dao.entity.SkillFile;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public interface ISkillFileHandler {

    void listByParent(RoutingContext context);

    void tree(RoutingContext context);

    void get(RoutingContext context);

    void createFolder(RoutingContext context);

    void createText(RoutingContext context);

    void uploadFile(RoutingContext context);

    void updateContent(RoutingContext context);

    void rename(RoutingContext context);

    void delete(RoutingContext context);
}
