-- 资源版本表：应用/处理器/工具 发布历史(append-only,最新 version 即当前生效版本)
CREATE TABLE "resource_version"
(
    "id"            UUID NOT NULL,
    "resource_type" VARCHAR(32),
    "resource_id"   UUID,
    "version"       INTEGER,
    "snapshot"      JSONB,
    "remark"        VARCHAR(512),
    "create_user"   UUID,
    "create_time"   TIMESTAMP,
    "update_time"   TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE INDEX "idx_resource_version_resource" ON "resource_version" ("resource_type", "resource_id", "version");

-- 存量应用各生成一条初始版本 v1(snapshot = {"workflow": <application.workflow>}),上线即兼容 latest-wins
INSERT INTO "resource_version" ("id", "resource_type", "resource_id", "version", "snapshot", "remark", "create_user", "create_time", "update_time")
SELECT gen_random_uuid(), 'application', a."id", 1,
       jsonb_build_object('workflow', COALESCE(a."workflow", '{}'::jsonb)),
       '初始版本', NULL, NOW(), NOW()
FROM "application" a;
