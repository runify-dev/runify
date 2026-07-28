package com.run.handler.version;

import com.run.dao.entity.ResourceVersion;
import com.run.dao.mapper.ResourceVersionMapper;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.run.sql.DSL.field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/7/28}
 * {@code @Version 1.0}
 * {@code @注释: 资源发布版本通用服务(application/processor/tool 共用)。
 * append-only：发布只追加,最新版本即生效版本;回滚由前端把某版本 snapshot 回填画布后重新发布。
 * 各资源"快照包含哪些内容"由各自 handler 决定(传入 snapshot),本服务只管版本机制。}
 */
public class VersionService {
    public static final String APPLICATION = "application";
    public static final String PROCESSOR = "processor";
    public static final String TOOL = "tool";

    private final ResourceVersionMapper mapper;

    @Inject
    public VersionService(ResourceVersionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 取某资源最新版本(latest = 生效版本);无版本返回 null
     */
    public Future<ResourceVersion> latest(String resourceType, UUID resourceId) {
        return mapper.list(mapper.select()
                        .where(field(ResourceVersion::getResourceType).eq(resourceType)
                                .and(field(ResourceVersion::getResourceId).eq(resourceId.toString())))
                        .orderBy(field(ResourceVersion::getVersion).desc())
                        .limit(1).render())
                .map(list -> list == null || list.isEmpty() ? null : list.getFirst());
    }

    /**
     * 发布：基于最新版本号 +1 追加一条新版本(永远累加)
     */
    public Future<ResourceVersion> publish(String resourceType, UUID resourceId,
                                           JsonObject snapshot, String remark, UUID createUser) {
        return latest(resourceType, resourceId).compose(latest -> {
            int next = latest == null ? 1 : latest.getVersion() + 1;
            LocalDateTime now = LocalDateTime.now();
            ResourceVersion version = new ResourceVersion(UUID.randomUUID(), resourceType, resourceId,
                    next, snapshot, remark, createUser, now, now);
            return mapper.save(version).map(_ -> version);
        });
    }

    /**
     * 发布历史：按版本号倒序
     */
    public Future<List<ResourceVersion>> list(String resourceType, UUID resourceId) {
        return mapper.list(mapper.select()
                .where(field(ResourceVersion::getResourceType).eq(resourceType)
                        .and(field(ResourceVersion::getResourceId).eq(resourceId.toString())))
                .orderBy(field(ResourceVersion::getVersion).desc()).render());
    }

    /**
     * 取单个版本(含 snapshot,供前端回滚回填画布)
     */
    public Future<ResourceVersion> get(UUID versionId) {
        return mapper.getById(versionId.toString());
    }

    /**
     * 应用运行时(线上/IM)使用的工作流 = 最新版本的 snapshot.workflow。
     * 未发布则失败(线上强制先发布,存量应用由迁移自动生成 v1)。
     */
    public Future<JsonObject> effectiveWorkflow(UUID applicationId) {
        return latest(APPLICATION, applicationId).map(v -> {
            if (v == null || v.getSnapshot() == null) {
                throw new RuntimeException("应用尚未发布");
            }
            return v.getSnapshot().getJsonObject("workflow");
        });
    }
}
