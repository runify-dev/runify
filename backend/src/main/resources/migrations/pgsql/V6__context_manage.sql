-- 上下文记忆表（便签 / 摘要 / 便签子区配置）：合并原 V6~V9 的最终结构，直接建终态
CREATE TABLE "ctx_fact"
(
    "id"             UUID NOT NULL,
    "scope_type"     VARCHAR(32),
    "scope_id"       VARCHAR(256),
    "subtype"        VARCHAR(64),
    "fact_key"       VARCHAR(1024),
    "fact_value"     TEXT,
    "provisional"    BOOLEAN,
    "create_time"    TIMESTAMP,
    "update_time"    TIMESTAMP,
    "application_id" VARCHAR(256),
    PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_ctx_fact_key" ON "ctx_fact" ("scope_type", "scope_id", "application_id", "subtype", "fact_key");

CREATE INDEX "idx_ctx_fact_gc" ON "ctx_fact" ("scope_type", "update_time");


CREATE TABLE "ctx_summary"
(
    "id"           UUID NOT NULL,
    "scope_type"   VARCHAR(32),
    "scope_id"     VARCHAR(256),
    "summary_text" TEXT,
    "covered_upto" TIMESTAMP,
    "create_time"  TIMESTAMP,
    "update_time"  TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_ctx_summary_scope" ON "ctx_summary" ("scope_type", "scope_id");


CREATE TABLE "ctx_section"
(
    "id"             UUID NOT NULL,
    "application_id" VARCHAR(128),
    "section_key"    VARCHAR(64),
    "label"          VARCHAR(128),
    "description"    TEXT,
    "scope"          VARCHAR(32),
    "list_style"     BOOLEAN,
    "enabled"        BOOLEAN,
    "sort_order"     INTEGER,
    "create_time"    TIMESTAMP,
    "update_time"    TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_ctx_section" ON "ctx_section" ("application_id", "section_key");
