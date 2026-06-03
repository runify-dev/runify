CREATE TABLE "skill"
(
    "id"                    VARCHAR(32) NOT NULL,
    "parent_id"             VARCHAR(32),
    "name"                  VARCHAR(256),
    "icon"                  VARCHAR(256),
    "desc"                  VARCHAR(512),
    "parameter_value"       TEXT,
    "skill_parameter_form"  TEXT,
    "create_time"           TIMESTAMP,
    "update_time"           TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_folder"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_relation"
(
    "id"            VARCHAR(32) NOT NULL,
    "ancestor_id"   VARCHAR(32),
    "descendant_id" VARCHAR(32),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_permission"
(
    "id"          VARCHAR(32) NOT NULL,
    "user_id"     VARCHAR(32),
    "target"      VARCHAR(255),
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_file"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "skill_id"    VARCHAR(32) NOT NULL,
    "name"        VARCHAR(256),
    "type"        VARCHAR(32),
    "content"     TEXT,
    "file_id"     VARCHAR(32),
    "file_name"   VARCHAR(512),
    "file_size"   BIGINT,
    "desc"        VARCHAR(512),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
