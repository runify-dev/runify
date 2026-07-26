-- 项目级 AI 生成的持久化：L1 蓝图 / L2 会话 / L3 任务台账 + 统一 append-only 消息流
CREATE TABLE "project_ai_blueprint"
(
    "id"          UUID NOT NULL,
    "project_id"  UUID NOT NULL,
    "description" TEXT,
    "conventions" TEXT,
    "memory"      JSONB,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX "uk_ai_blueprint_project" ON "project_ai_blueprint" ("project_id");


CREATE TABLE "project_ai_session"
(
    "id"              UUID NOT NULL,
    "project_id"      UUID NOT NULL,
    "title"           VARCHAR(256),
    "status"          VARCHAR(32),
    "summary"         TEXT,
    "facts"           JSONB,
    "window_from_seq" BIGINT,
    "timeline"        JSONB,
    "create_time"     TIMESTAMP,
    "update_time"     TIMESTAMP,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_ai_session_project" ON "project_ai_session" ("project_id");


CREATE TABLE "project_ai_task"
(
    "id"              UUID NOT NULL,
    "session_id"      UUID NOT NULL,
    "project_id"      UUID NOT NULL,
    "processor_id"    UUID,
    "requirement"     TEXT,
    "status"          VARCHAR(32),
    "summary"         TEXT,
    "facts"           JSONB,
    "window_from_seq" BIGINT,
    "workflow"        JSONB,
    "result"          JSONB,
    "timeline"        JSONB,
    "create_time"     TIMESTAMP,
    "update_time"     TIMESTAMP,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_ai_task_session" ON "project_ai_task" ("session_id");
CREATE INDEX "idx_ai_task_project" ON "project_ai_task" ("project_id");


CREATE TABLE "project_ai_message"
(
    "id"          UUID        NOT NULL,
    "owner_type"  VARCHAR(16) NOT NULL,
    "owner_id"    UUID        NOT NULL,
    "seq"         BIGINT      NOT NULL,
    "payload"     JSONB,
    "token_count" INTEGER,
    "compacted"   BOOLEAN,
    "create_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX "uk_ai_message_owner_seq" ON "project_ai_message" ("owner_type", "owner_id", "seq");
