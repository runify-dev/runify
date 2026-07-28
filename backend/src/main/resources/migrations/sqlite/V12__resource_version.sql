-- 资源版本表：应用/处理器/工具 发布历史(append-only,最新 version 即当前生效版本)
CREATE TABLE "resource_version"
(
    "id"            TEXT(32) NOT NULL,
    "resource_type" TEXT(32),
    "resource_id"   TEXT(32),
    "version"       integer,
    "snapshot"      TEXT,
    "remark"        TEXT(512),
    "create_user"   TEXT(32),
    "create_time"   TIMESTAMP,
    "update_time"   TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE INDEX "idx_resource_version_resource" ON "resource_version" ("resource_type", "resource_id", "version");

-- 存量应用各生成一条初始版本 v1(snapshot = {"workflow": <application.workflow>}),上线即兼容 latest-wins
INSERT INTO "resource_version" ("id", "resource_type", "resource_id", "version", "snapshot", "remark", "create_user", "create_time", "update_time")
SELECT lower(substr(hex(randomblob(16)), 1, 8) || '-' || substr(hex(randomblob(16)), 1, 4) || '-' ||
             substr(hex(randomblob(16)), 1, 4) || '-' || substr(hex(randomblob(16)), 1, 4) || '-' ||
             substr(hex(randomblob(16)), 1, 12)),
       'application', a."id", 1,
       '{"workflow":' || COALESCE(NULLIF(a."workflow", ''), '{}') || '}',
       '初始版本', NULL, datetime('now'), datetime('now')
FROM "application" a;
