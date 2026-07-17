package com.run.handler.knowledge;

import com.run.dao.entity.Document;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public interface IDocumentHandler {

    void listByParent(RoutingContext context);

    void tree(RoutingContext context);

    void get(RoutingContext context);

    void createFolder(RoutingContext context);

    void createText(RoutingContext context);

    void importDocument(RoutingContext context);

    void updateContent(RoutingContext context);

    void rename(RoutingContext context);

    void delete(RoutingContext context);
}
