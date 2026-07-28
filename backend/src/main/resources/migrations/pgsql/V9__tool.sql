CREATE TABLE "tool"
(
    "id"            UUID NOT NULL,
    "parent_id"     UUID,
    "name"          VARCHAR(256),
    "label"         VARCHAR(256),
    "description"   VARCHAR(1024),
    "icon"          VARCHAR(256),
    "runtime"       VARCHAR(32),
    "input_schema"  TEXT,
    "output_schema" TEXT,
    "config_schema" TEXT,
    "config"        TEXT,
    "body"          TEXT,
    "create_time"   TIMESTAMP,
    "update_time"   TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "tool_folder"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "tool_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "tool_permission"
(
    "id"          UUID NOT NULL,
    "user_id"     UUID,
    "target"      UUID,
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
