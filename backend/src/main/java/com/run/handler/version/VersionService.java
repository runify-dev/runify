package com.run.handler.version;

import com.run.dao.entity.ResourceVersion;
import com.run.dao.entity.User;
import com.run.dao.mapper.ResourceVersionMapper;
import com.run.dao.mapper.UserMapper;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final UserMapper userMapper;

    @Inject
    public VersionService(ResourceVersionMapper mapper, UserMapper userMapper) {
        this.mapper = mapper;
        this.userMapper = userMapper;
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
     * 发布历史 VO(不含 snapshot),并用一条 in 查询批量补充发布人名称
     * (存量 seed 或用户已删时 create_user 名称留空)
     */
    public Future<List<VersionVO>> listVOs(String resourceType, UUID resourceId) {
        return list(resourceType, resourceId).compose(versions -> {
            List<String> userIds = versions.stream()
                    .map(ResourceVersion::getCreateUser)
                    .filter(Objects::nonNull)
                    .map(UUID::toString)
                    .distinct()
                    .toList();
            Future<Map<UUID, String>> nameMapFuture = userIds.isEmpty()
                    ? Future.succeededFuture(Map.of())
                    : userMapper.list(field(User::getId).in(userIds)).map(users -> users.stream()
                            .collect(Collectors.toMap(User::getId,
                                    u -> u.getNickname() != null ? u.getNickname() : u.getUsername())));
            return nameMapFuture.map(nameMap -> versions.stream()
                    .map(v -> new VersionVO(v.getId(), v.getVersion(), v.getRemark(),
                            v.getCreateUser(),
                            v.getCreateUser() == null ? null : nameMap.get(v.getCreateUser()),
                            v.getCreateTime()))
                    .toList());
        });
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
