-- 项目级 AI 生成的持久化：L1 蓝图 / L2 会话 / L3 任务台账 + 统一 append-only 消息流
CREATE TABLE "project_ai_blueprint" (
    "id"          text(32) NOT NULL,
    "project_id"  text(32) NOT NULL,
    "description" TEXT,
    "conventions" TEXT,
    "memory"      TEXT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX "uk_ai_blueprint_project" ON "project_ai_blueprint" ("project_id");


CREATE TABLE "project_ai_session" (
    "id"              text(32) NOT NULL,
    "project_id"      text(32) NOT NULL,
    "title"           text(256),
    "status"          text(32),
    "summary"         TEXT,
    "facts"           TEXT,
    "window_from_seq" BIGINT,
    "timeline"        TEXT,
    "create_time"     TIMESTAMP,
    "update_time"     TIMESTAMP,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_ai_session_project" ON "project_ai_session" ("project_id");


CREATE TABLE "project_ai_task" (
    "id"              text(32) NOT NULL,
    "session_id"      text(32) NOT NULL,
    "project_id"      text(32) NOT NULL,
    "processor_id"    text(32),
    "requirement"     TEXT,
    "status"          text(32),
    "summary"         TEXT,
    "facts"           TEXT,
    "window_from_seq" BIGINT,
    "workflow"        TEXT,
    "result"          TEXT,
    "timeline"        TEXT,
    "create_time"     TIMESTAMP,
    "update_time"     TIMESTAMP,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_ai_task_session" ON "project_ai_task" ("session_id");
CREATE INDEX "idx_ai_task_project" ON "project_ai_task" ("project_id");


CREATE TABLE "project_ai_message" (
    "id"          text(32) NOT NULL,
    "owner_type"  text(16) NOT NULL,
    "owner_id"    text(32) NOT NULL,
    "seq"         BIGINT NOT NULL,
    "payload"     TEXT,
    "token_count" INTEGER,
    "compacted"   integer(2),
    "create_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX "uk_ai_message_owner_seq" ON "project_ai_message" ("owner_type", "owner_id", "seq");
