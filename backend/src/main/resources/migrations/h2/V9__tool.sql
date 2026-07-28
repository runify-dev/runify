CREATE TABLE "tool"
(
    "id"            VARCHAR(32) NOT NULL,
    "parent_id"     VARCHAR(32),
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
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "tool_relation"
(
    "id"            VARCHAR(32) NOT NULL,
    "ancestor_id"   VARCHAR(32),
    "descendant_id" VARCHAR(32),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "tool_permission"
(
    "id"          VARCHAR(32) NOT NULL,
    "user_id"     VARCHAR(32),
    "target"      VARCHAR(255),
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
