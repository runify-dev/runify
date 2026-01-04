package com.run.common.project.executor;

import com.fasterxml.jackson.core.JsonGenerator;
import com.run.common.constants.ProcessorProtocolConstants;
import com.run.common.project.ProjectManage;
import com.run.dao.entity.Processor;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.WorkFlow;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/29  22:58}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class HttpProcessorExecutor extends ProcessorExecutor {
    private Route route;

    public HttpProcessorExecutor(Processor processor, ProjectManage.ProjectExecutor projectExecutor) {
        super(processor, projectExecutor);
    }


    @Override
    public Boolean unDeploy() {
        this.route.remove();
        this.route = null;
        return super.unDeploy();
    }

    @Override
    public Boolean deploy() {
        Router router = projectExecutor.getRouter();
        JsonObject meta = processor.getMeta();
        String method = meta.getString("method");
        this.route = router.route(HttpMethod.valueOf(method), meta.getString("path"))
                .handler(this::handler);
        return Boolean.TRUE;
    }

    public void handler(RoutingContext context) {
        JsonObject workflow = processor.getWorkflow();

        WorkFlowManage workFlowManage = new WorkFlowManage(WorkFlow.of(workflow, WorkflowType.PROCESSOR_HTTP),
                new HashMap<String, Object>() {{
                    put("context", context);
                    put("pools", projectExecutor.getPools());
                }},
                new HashMap<>(), (wm, node, chunk, aBoolean) -> {
            if (aBoolean) {
                boolean isJsonGenerator = wm.getParams().containsKey("jsonGenerator");
                if (isJsonGenerator) {
                    JsonGenerator jsonGenerator = (JsonGenerator) wm.getParams().get("jsonGenerator");
                    try {
                        jsonGenerator.writeEndObject();
                        jsonGenerator.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    context.end();
                }

            }
        });
        workFlowManage.invoke();
    }
}
