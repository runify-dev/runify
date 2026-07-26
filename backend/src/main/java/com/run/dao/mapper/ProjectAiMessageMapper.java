package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ProjectAiMessage;
import com.run.sql.condition.Condition;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.Objects;

import static com.run.sql.DSL.field;

public class ProjectAiMessageMapper extends BaseMapper<ProjectAiMessage> {
    @Inject
    public ProjectAiMessageMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }

    /**
     * 某 owner（会话 / 任务）下的下一个 seq = max(seq)+1，空则从 0 起。
     * 由服务端权威分配，配合 (owner_type, owner_id, seq) 唯一索引兜住并发重号。
     */
    public Future<Long> nextSeq(String ownerType, String ownerId) {
        Condition condition = field(ProjectAiMessage::getOwnerType).eq(ownerType)
                .and(field(ProjectAiMessage::getOwnerId).eq(ownerId));
        return list(condition).map(rows -> rows.stream()
                .map(ProjectAiMessage::getSeq)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(max -> max + 1)
                .orElse(0L));
    }
}
