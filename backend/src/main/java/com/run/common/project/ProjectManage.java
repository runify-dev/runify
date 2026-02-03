package com.run.common.project;

import com.run.common.constants.DatabaseConnectionProtocolConstants;
import com.run.common.constants.ProcessorProtocolConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.project.executor.HttpProcessorExecutor;
import com.run.common.project.executor.ProcessorExecutor;
import com.run.common.project.pool.PostgreSQL;
import com.run.dao.common.F;
import com.run.dao.entity.DatabaseConnectionPool;
import com.run.dao.entity.Processor;
import com.run.dao.entity.Project;
import com.run.dao.mapper.DatabaseConnectionPoolMapper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ProjectManage {
    private static Router mainRouter;
    private static Vertx vertx;
    private static DatabaseConnectionPoolMapper databaseConnectionPoolMapper;
    private static Supplier<Router> getChildRouter;
    private static final Map<ProcessorProtocolConstants, BiFunction<Processor, ProjectExecutor, ProcessorExecutor>> processorExecutorNewInstanceMap = Map.of(ProcessorProtocolConstants.HTTP, HttpProcessorExecutor::new);
    private static final Map<DatabaseConnectionProtocolConstants, BiFunction<DatabaseConnectionPool, Vertx, Pool>> poolNewInstance = Map.of(DatabaseConnectionProtocolConstants.POSTGRESQL, PostgreSQL::toPool);
    private static final ConcurrentMap<UUID, ProjectExecutor> processorMap = new ConcurrentHashMap<>();

    public static void setDatabaseConnectionPoolMapper(DatabaseConnectionPoolMapper databaseConnectionPoolMapper) {
        ProjectManage.databaseConnectionPoolMapper = databaseConnectionPoolMapper;
    }

    public static void setRouter(Router router) {
        ProjectManage.mainRouter = router;
    }

    public static void setVertx(Vertx vertx) {
        ProjectManage.vertx = vertx;
    }

    public static void setGetChildRouter(Supplier<Router> getChildRouter) {
        ProjectManage.getChildRouter = getChildRouter;
    }

    public static ProcessorExecutor generateProcessorExecutor(Project project, Processor processor) {
        ProjectExecutor projectExecutor = processorMap.computeIfAbsent(project.getId(), key -> new ProjectExecutor(key, project));
        return projectExecutor.generateProcessorExecutor(processor.getId(), processor);
    }

    public static Boolean updatePool(UUID projectId, DatabaseConnectionPool pool) {
        ProjectExecutor projectExecutor = processorMap.get(projectId);
        if (projectExecutor == null) {
            return Boolean.FALSE;
        }
        return projectExecutor.updatePool(pool);
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
        private final ConcurrentMap<String, Pool> pools = new ConcurrentHashMap<>();
        private final ConcurrentMap<UUID, ProcessorExecutor> processorMap = new ConcurrentHashMap<>();

        public Boolean updatePool(DatabaseConnectionPool pool) {
            Pool p = poolNewInstance.get(pool.getProtocol()).apply(pool, vertx);
            pools.put(pool.getId().toString(), p);
            return Boolean.TRUE;
        }

        public ProjectExecutor(UUID id, Project project) {
            this.id = id;
            this.project = project;
            String path = this.project.getPath() + "/*";
            this.router = getChildRouter.get();
            mainRouter.route(path).subRouter(this.router);
            databaseConnectionPoolMapper
                    .search(F.field(DatabaseConnectionPool::getProjectId).eq(F.params(DatabaseConnectionPool::getProjectId)),
                            Map.of("projectId", this.project.getId().toString()))
                    .onSuccess(poolList -> {
                        Map<String, Pool> collect = poolList.stream()
                                .map(databaseConnectionPool -> new DefaultKeyValue<>(databaseConnectionPool.getId().toString(), poolNewInstance.get(databaseConnectionPool.getProtocol()).apply(databaseConnectionPool, vertx)))
                                .collect(Collectors.toMap(DefaultKeyValue::getKey, DefaultKeyValue::getValue));
                        pools.putAll(collect);
                    });
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

