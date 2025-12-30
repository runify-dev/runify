package com.run.common.project;

import com.run.common.constants.ProcessorProtocolConstants;
import com.run.common.project.executor.HttpProcessorExecutor;
import com.run.common.project.executor.ProcessorExecutor;
import com.run.dao.entity.Processor;
import com.run.dao.entity.Project;
import io.vertx.ext.web.Router;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ProjectManage {
    private static Router mainRouter;
    private static Supplier<Router> getChildRouter;
    private static final Map<ProcessorProtocolConstants, BiFunction<Processor, ProjectExecutor, ProcessorExecutor>> processorExecutorNewInstanceMap = Map.of(ProcessorProtocolConstants.HTTP, HttpProcessorExecutor::new);
    private static final ConcurrentMap<UUID, ProjectExecutor> processorMap = new ConcurrentHashMap<>();

    public static void setRouter(Router router) {
        ProjectManage.mainRouter = router;
    }

    public static void setGetChildRouter(Supplier<Router> getChildRouter) {
        ProjectManage.getChildRouter = getChildRouter;
    }

    public static ProcessorExecutor generateProcessorExecutor(Project project, Processor processor) {
        ProjectExecutor projectExecutor = processorMap.computeIfAbsent(project.getId(), key -> new ProjectExecutor(key, project));
        return projectExecutor.generateProcessorExecutor(processor.getId(), processor);
    }

    public static ProcessorExecutor getProcessorExecutor(UUID projectId, UUID processorId) {
        ProjectExecutor projectExecutor = processorMap.get(projectId);
        if (projectExecutor == null) {
            return null;
        }
        return projectExecutor.getProcessorExecutor(processorId);
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
        private Router router;
        ConcurrentMap<UUID, ProcessorExecutor> processorMap = new ConcurrentHashMap<>();

        public ProjectExecutor(UUID id, Project project) {
            this.id = id;
            this.project = project;
            String path = this.project.getPath() + "/*";
            this.router = getChildRouter.get();
            mainRouter.route(path).subRouter(this.router);
        }

        public ProcessorExecutor generateProcessorExecutor(UUID processorId, Processor processor) {
            return processorMap.computeIfAbsent(processorId, p -> processorExecutorNewInstanceMap.get(processor.getProtocol()).apply(processor, this));
        }

        public ProcessorExecutor getProcessorExecutor(UUID processorId) {
            return processorMap.get(processorId);
        }

        public boolean isDeploy(UUID processorId) {
            return processorMap.containsKey(processorId);
        }

        public Boolean unDeploy(UUID processorId) {
            ProcessorExecutor processorExecutor = processorMap.remove(processorId);
            if (processorExecutor != null) {
                return processorExecutor.unDeploy();
            }
            return Boolean.TRUE;
        }
    }

}

