CREATE TABLE "tool"
(
    "id"            text NOT NULL,
    "parent_id"     text,
    "name"          text,
    "label"         text,
    "description"   text,
    "icon"          text,
    "runtime"       text,
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
    "id"          text NOT NULL,
    "parent_id"   text,
    "name"        text,
    "desc"        text,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "tool_relation"
(
    "id"            text NOT NULL,
    "ancestor_id"   text,
    "descendant_id" text,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "tool_permission"
(
    "id"          text NOT NULL,
    "user_id"     text,
    "target"      text,
    "permission"  text,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
