CREATE TABLE "user"
(
    "id"          VARCHAR(32)              NOT NULL,
    "email"       VARCHAR(255)             NOT NULL,
    "phone"       VARCHAR(32),
    "nick_name"   VARCHAR(255)             NOT NULL,
    "username"    VARCHAR(255)             NOT NULL,
    "icon"        VARCHAR(255),
    "role"        VARCHAR(32)              NOT NULL,
    "password"    VARCHAR(255)             NOT NULL,
    "create_time" TIMESTAMP WITH TIME ZONE NOT NULL,
    "update_time" TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY ("id")
);

INSERT INTO "user" ("id", "email", "phone", "nick_name", "username", "role", "password", "create_time", "update_time",
                    "icon")
VALUES ('22d90f6c-2092-43b8-aa14-d1f9731522ac', '', NULL, '系统管理员', 'admin', 'ADMIN',
        '32d991775d14e9fa31a3633eb3cd253d5c1ecfae8b64dc6d7391a29ccc6fd824', '2022-04-17 00:59:01',
        '2025-04-05 00:00:00', './user.png');

CREATE TABLE "file"
(
    "id"          VARCHAR(32)  NOT NULL,
    "file_name"   VARCHAR(255),
    "lo_id"       BIGINT,
    "sha256_hash" VARCHAR(255),
    "ref_type"    VARCHAR(64),
    "ref"         VARCHAR(32),
    "meta"        TEXT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    "path"        TEXT,
    "size"        BIGINT,
    PRIMARY KEY ("id")
);


CREATE TABLE "application_relation"
(
    "id"            VARCHAR(32) NOT NULL,
    "ancestor_id"   VARCHAR(32),
    "descendant_id" VARCHAR(32),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "application"
(
    "id"                     VARCHAR(32) NOT NULL,
    "parent_id"              VARCHAR(32),
    "name"                   VARCHAR(256),
    "icon"                   VARCHAR(256),
    "desc"                   VARCHAR(256),
    "workflow"               TEXT,
    "setting"                TEXT,
    "star"                   SMALLINT,
    "share"                  SMALLINT,
    "allow_anonymous_access" SMALLINT,
    "create_time"            TIMESTAMP,
    "update_time"            TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "application_folder"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "application_permission"
(
    "id"          VARCHAR(32) NOT NULL,
    "user_id"     VARCHAR(32),
    "target"      VARCHAR(255),
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "model"
(
    "id"                   VARCHAR(32) NOT NULL,
    "parent_id"            VARCHAR(32),
    "name"                 VARCHAR(256),
    "icon"                 VARCHAR(256),
    "desc"                 VARCHAR(256),
    "provider"             VARCHAR(128),
    "model_type"           VARCHAR(256),
    "model_name"           VARCHAR(256),
    "credential"           TEXT,
    "model_parameter_form" TEXT,
    "meta"                 TEXT,
    "star"                 SMALLINT,
    "share"                SMALLINT,
    "create_time"          TIMESTAMP,
    "update_time"          TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "model_folder"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "model_relation"
(
    "id"            VARCHAR(32) NOT NULL,
    "ancestor_id"   VARCHAR(32),
    "descendant_id" VARCHAR(32),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "model_permission"
(
    "id"          VARCHAR(32) NOT NULL,
    "user_id"     VARCHAR(32),
    "target"      VARCHAR(255),
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);


CREATE TABLE "system_setting"
(
    "type" VARCHAR(64) NOT NULL,
    "meta" TEXT,
    PRIMARY KEY ("type")
);


CREATE TABLE "note"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "name"        VARCHAR(256),
    "icon"        VARCHAR(256),
    "content"     TEXT,
    "excerpt"     VARCHAR(256),
    "star"        SMALLINT,
    "share"       SMALLINT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "note_folder"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "note_relation"
(
    "id"            VARCHAR(32) NOT NULL,
    "ancestor_id"   VARCHAR(32),
    "descendant_id" VARCHAR(32),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);
CREATE TABLE "note_permission"
(
    "id"          VARCHAR(32) NOT NULL,
    "user_id"     VARCHAR(32),
    "target"      VARCHAR(255),
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "conversation"
(
    "id"                     VARCHAR(32) NOT NULL,
    "name"                   VARCHAR(256),
    "application_id"         VARCHAR(32),
    "is_deleted"             SMALLINT,
    "meta"                   TEXT,
    "conversation_user_id"   VARCHAR(32),
    "conversation_user_type" VARCHAR(64),
    "execute_type"           VARCHAR(64),
    "star_num"               INTEGER,
    "trample_num"            INTEGER,
    "mark_sum"               INTEGER,
    "conversation_count"     INTEGER,
    "create_time"            TIMESTAMP,
    "update_time"            TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "conversation_message"
(
    "id"                VARCHAR(128) NOT NULL,
    "application_id"    VARCHAR(128),
    "conversation_id"   VARCHAR(128),
    "workflow_run_id"   VARCHAR(128),
    "type"              VARCHAR(20),
    "content"           TEXT,
    "context"           TEXT,
    "prompt_tokens"     INTEGER,
    "completion_tokens" INTEGER,
    "duration"          INTEGER,
    "create_time"       TIMESTAMP,
    "update_time"       TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "project"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "desc"        VARCHAR(256),
    "name"        VARCHAR(256),
    "icon"        VARCHAR(256),
    "path"        TEXT,
    "star"        SMALLINT,
    "share"       SMALLINT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);


CREATE TABLE "project_folder"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);


CREATE TABLE "project_relation"
(
    "id"            VARCHAR(32) NOT NULL,
    "ancestor_id"   VARCHAR(32),
    "descendant_id" VARCHAR(32),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "project_permission"
(
    "id"          VARCHAR(32) NOT NULL,
    "user_id"     VARCHAR(32),
    "target"      VARCHAR(255),
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "processor"
(
    "id"          VARCHAR(32) NOT NULL,
    "project_id"  VARCHAR(32) NOT NULL,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "protocol"    VARCHAR(32),
    "meta"        TEXT,
    "workflow"    TEXT,
    "activate"    SMALLINT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "role"
(
    "id"          VARCHAR(32) NOT NULL,
    "name"        VARCHAR(255),
    "internal"    INTEGER,
    "type"        VARCHAR(64),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "role_permission_relation"
(
    "id"            VARCHAR(32) NOT NULL,
    "role_id"       VARCHAR(32),
    "permission_id" VARCHAR(255),
    PRIMARY KEY ("id")
);

CREATE TABLE "role_user_relation"
(
    "id"      VARCHAR(32) NOT NULL,
    "role_id" VARCHAR(32),
    "user_id" VARCHAR(32),
    PRIMARY KEY ("id")
);

INSERT INTO "role" ("id", "name", "internal", "type", "create_time", "update_time")
VALUES ('ADMIN', 'ADMIN', 1, 'ADMIN', '2026-04-16 23:00:01', '2026-04-16 23:00:01'),
       ('USER', 'USER', 1, 'USER', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO "role_user_relation" ("id", "role_id", "user_id")
VALUES ('1', 'ADMIN', '22d90f6c-2092-43b8-aa14-d1f9731522ac');

CREATE TABLE "datasource"
(
    "id"               VARCHAR(32) NOT NULL,
    "parent_id"        VARCHAR(32),
    "name"             VARCHAR(256),
    "desc"             VARCHAR(256),
    "data_source_type" VARCHAR(32),
    "provider"         VARCHAR(32),
    "meta"             TEXT,
    "create_time"      TIMESTAMP,
    "update_time"      TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "datasource_folder"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "datasource_relation"
(
    "id"            VARCHAR(32) NOT NULL,
    "ancestor_id"   VARCHAR(32),
    "descendant_id" VARCHAR(32),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "datasource_permission"
(
    "id"          VARCHAR(32) NOT NULL,
    "user_id"     VARCHAR(32),
    "target"      VARCHAR(255),
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
