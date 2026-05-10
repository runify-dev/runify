CREATE TABLE "datasource"
(
    "id"               text(32) NOT NULL,
    "parent_id"        text(32),
    "name"             TEXT(256),
    "desc"             TEXT(256),
    "data_source_type" TEXT(32),
    "provider"         TEXT(32),
    "meta"             TEXT,
    "create_time"      TIMESTAMP,
    "update_time"      TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "datasource_folder"
(
    "id"          TEXT(32) NOT NULL,
    "parent_id"   TEXT(32),
    "name"        TEXT(256),
    "desc"        TEXT(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "datasource_relation"
(
    "id"            text(32) NOT NULL,
    "ancestor_id"   text(32),
    "descendant_id" text(32),
    "depth"         integer(10),
    PRIMARY KEY ("id")
);

CREATE TABLE "datasource_permission"
(
    "id"          text NOT NULL,
    "user_id"     text,
    "target"      TEXT,
    "permission"  TEXT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
