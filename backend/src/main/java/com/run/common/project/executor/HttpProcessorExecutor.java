package com.run.common.project.executor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.run.common.project.ProjectManage;
import com.run.common.util.JacksonUtils;
import com.run.dao.entity.Processor;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.*;
import com.run.workflow.message.struct.chunk.FailureContentChunk;
import com.run.workflow.message.struct.chunk.JsonContentChunk;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/29  22:58}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class HttpProcessorExecutor extends ProcessorExecutor {
    private Route route;
    private ConcurrentHashMap<UUID, JsonGenerator> jsonGeneratorMap = new ConcurrentHashMap<>();
    private WorkFlow workFlowInstance;

    public HttpProcessorExecutor(Processor processor, ProjectManage.ProjectExecutor projectExecutor) {
        super(processor, projectExecutor);
        this.workFlowInstance = WorkFlow.of(processor.getWorkflow(), WorkflowType.PROCESSOR_HTTP);
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
                .handler(BodyHandler.create().setBodyLimit(Long.MAX_VALUE)
                        .setDeleteUploadedFilesOnEnd(true))
                .handler(this::handler);
        return Boolean.TRUE;
    }

    public JsonGenerator newJsonGenerator(RoutingContext context) {
        JsonFactory factory = new JsonFactory();
        try {
            JsonGenerator generator = factory.createGenerator(new Writer() {
                @Override
                public void write(@NotNull char[] cbuf, int off, int len) {
                    context.response().write(new String(cbuf, off, len));
                }

                @Override
                public void flush() throws IOException {

                }

                @Override
                public void close() throws IOException {

                }
            });
            generator.writeStartObject();
            return generator;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handler(RoutingContext context) {
        context.response().setChunked(true);
        UUID requestId = UUID.randomUUID();
        WorkFlowManage workFlowManage = new WorkFlowManage(workFlowInstance,
                new HashMap<>() {{
                    put("pools", projectExecutor.getPools());
                    put("context", context);
                }},
                new HashMap<>(), (wm, node, chunk, aBoolean) -> {
            if (aBoolean) {
                boolean contains = jsonGeneratorMap.containsKey(requestId);
                if (contains) {
                    JsonGenerator jsonGenerator = jsonGeneratorMap.get(requestId);
                    try {
                        jsonGenerator.writeEndObject();
                        jsonGenerator.flush();
                        jsonGenerator.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    jsonGeneratorMap.remove(requestId, jsonGenerator);
                    context.end();
                } else {
                    context.end();
                }
                return;
            }
            for (AnswerContent c : chunk.content()) {
                if (c instanceof JsonContentChunk contentChunk) {
                    // 这里就直接响应
                    context.end(contentChunk.getContent());
                }
                if (c instanceof StatusContent statusContent) {
                    if (!context.response().headWritten()) {
                        context.response().setStatusCode(statusContent.getContent());
                    }
                }
                if (c instanceof HeadersContent headersContent) {
                    if (!context.response().headWritten()) {
                        Map<String, String> content = headersContent.getContent();
                        for (Map.Entry<String, String> header : content.entrySet()) {
                            context.response().putHeader(header.getKey(), header.getValue());
                        }
                    }

                }
                if (c instanceof TextContent textContent) {
                    context.end(textContent.getContent());
                }
                if (c instanceof JsonFieldsContent jsonFieldsContent) {
                    JsonGenerator jsonGenerator = jsonGeneratorMap.computeIfAbsent(requestId, key -> newJsonGenerator(context));
                    Map<String, Object> content = jsonFieldsContent.getContent();
                    for (Map.Entry<String, Object> param : content.entrySet()) {
                        try {
                            jsonGenerator.writeFieldName(param.getKey());
                            jsonGenerator.writeRawValue(JacksonUtils.toJson(param.getValue()));
                        } catch (IOException e) {
                            context.response().write(e.getMessage());
                        }
                    }
                }
                if (c instanceof FailureContentChunk failureContentChunk) {
                    boolean contains = jsonGeneratorMap.containsKey(requestId);
                    String content = failureContentChunk.getContent();
                    if (contains) {
                        JsonGenerator jsonGenerator = jsonGeneratorMap.get(requestId);
                        try {
                            jsonGenerator.writeFieldName("message");
                            jsonGenerator.writeRawValue(JacksonUtils.toJson(content));
                            jsonGenerator.writeFieldName("code");
                            jsonGenerator.writeNumber(500);
                            jsonGenerator.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        context.response().write(content);
                    }

                }
            }

        });
        workFlowManage.invoke();
    }
}
