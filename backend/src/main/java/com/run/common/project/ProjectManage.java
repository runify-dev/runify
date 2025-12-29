package com.run.common.project;

import com.run.dao.entity.Processor;
import com.run.dao.entity.Project;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.WorkFlow;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProjectManage {
    private static final ConcurrentMap<UUID, ProjectExecutor> processorMap = new ConcurrentHashMap<>();

    public static ProcessorExecutor generateProcessorExecutor(Project project, Processor processor) {
        ProjectExecutor projectExecutor = processorMap.computeIfAbsent(project.getId(), key -> new ProjectExecutor(key, project));
        return projectExecutor.generateProcessorExecutor(processor.getId(), processor);
    }

    public static Boolean isDeploy(UUID projectId, UUID processorId) {
        return processorMap.containsKey(projectId) &&
                processorMap.get(projectId).isDeploy(processorId);

    }

    public static Boolean unDeploy(UUID projectId, UUID processorId) {
        if (processorMap.containsKey(projectId)) {
            return processorMap.get(projectId).unDeploy(processorId);
        }
        return Boolean.TRUE;
    }

    @Getter
    public static class ProjectExecutor {
        private UUID id;
        private Project project;
        ConcurrentMap<UUID, ProcessorExecutor> processorMap = new ConcurrentHashMap<>();

        public ProjectExecutor(UUID id, Project project) {
            this.id = id;
            this.project = project;
        }

        public ProcessorExecutor generateProcessorExecutor(UUID processorId, Processor processor) {
            return processorMap.computeIfAbsent(processorId, p -> new ProcessorExecutor(processorId, processor, this));
        }

        public boolean isDeploy(UUID processorId) {
            return processorMap.containsKey(processorId);
        }

        public Boolean unDeploy(UUID processorId) {
            processorMap.remove(processorId);
            return Boolean.TRUE;
        }
    }

    @Getter
    public static class ProcessorExecutor {
        private UUID id;
        private Processor processor;
        private ProjectExecutor projectExecutor;

        public ProcessorExecutor(UUID id, Processor processor, ProjectExecutor projectExecutor) {
            this.id = id;
            this.processor = processor;
            this.projectExecutor = projectExecutor;
        }

        public void handler(RoutingContext context) {
            JsonObject workflow = processor.getWorkflow();
            WorkFlowManage workFlowManage = new WorkFlowManage(WorkFlow.of(workflow, WorkflowType.PROCESSOR_HTTP),
                    Map.of("context", context),
                    new HashMap<>(), (wm, node, chunk, aBoolean) -> {
            });
            workFlowManage.invoke();
        }

    }
}

