CREATE TABLE "user"
(
    "id"          UUID              NOT NULL,
    "email"       VARCHAR(255)             NOT NULL,
    "phone"       VARCHAR(32),
    "nick_name"   VARCHAR(255)             NOT NULL,
    "username"    VARCHAR(255)             NOT NULL,
    "icon"        VARCHAR(255),
    "role"        VARCHAR(32)              NOT NULL,
    "password"    VARCHAR(255)             NOT NULL,
    "create_time" TIMESTAMP NOT NULL,
    "update_time" TIMESTAMP NOT NULL,
    PRIMARY KEY ("id")
);

INSERT INTO "user" ("id", "email", "phone", "nick_name", "username", "role", "password", "create_time", "update_time",
                    "icon")
VALUES ('22d90f6c-2092-43b8-aa14-d1f9731522ac', '', NULL, '系统管理员', 'admin', 'ADMIN',
        '32d991775d14e9fa31a3633eb3cd253d5c1ecfae8b64dc6d7391a29ccc6fd824', '2022-04-17 00:59:01',
        '2025-04-05 00:00:00', './user.png');

CREATE TABLE "file"
(
    "id"          UUID           NOT NULL,
    "file_name"   VARCHAR(255),
    "lo_id"       BIGINT,
    "sha256_hash" VARCHAR(255),
    "ref_type"    VARCHAR(64),
    "ref"         VARCHAR(255),
    "meta"        JSONB,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    "path"        TEXT,
    "size"        BIGINT,
    PRIMARY KEY ("id")
);


CREATE TABLE "application_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "application"
(
    "id"                     UUID NOT NULL,
    "parent_id"              UUID,
    "name"                   VARCHAR(256),
    "icon"                   VARCHAR(256),
    "desc"                   VARCHAR(256),
    "workflow"               JSONB,
    "setting"                JSONB,
    "star"                   BOOLEAN,
    "share"                  BOOLEAN,
    "allow_anonymous_access" BOOLEAN,
    "create_time"            TIMESTAMP,
    "update_time"            TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "application_folder"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "application_permission"
(
    "id"          UUID NOT NULL,
    "user_id"     UUID,
    "target"      UUID,
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "model"
(
    "id"                   UUID NOT NULL,
    "parent_id"            UUID,
    "name"                 VARCHAR(256),
    "icon"                 VARCHAR(256),
    "desc"                 VARCHAR(256),
    "provider"             VARCHAR(128),
    "model_type"           VARCHAR(256),
    "model_name"           VARCHAR(256),
    "credential"           TEXT,
    "model_parameter_form" JSONB,
    "meta"                 JSONB,
    "star"                 BOOLEAN,
    "share"                BOOLEAN,
    "create_time"          TIMESTAMP,
    "update_time"          TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "model_folder"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "model_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "model_permission"
(
    "id"          UUID NOT NULL,
    "user_id"     UUID,
    "target"      UUID,
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);


CREATE TABLE "system_setting"
(
    "type" VARCHAR(64) NOT NULL,
    "meta" JSONB,
    PRIMARY KEY ("type")
);


CREATE TABLE "note"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "icon"        VARCHAR(256),
    "content"     TEXT,
    "excerpt"     VARCHAR(256),
    "star"        BOOLEAN,
    "share"       BOOLEAN,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "note_folder"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "note_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);
CREATE TABLE "note_permission"
(
    "id"          UUID NOT NULL,
    "user_id"     UUID,
    "target"      UUID,
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "conversation"
(
    "id"                     UUID NOT NULL,
    "name"                   VARCHAR(256),
    "application_id"         UUID,
    "is_deleted"             BOOLEAN,
    "meta"                   JSONB,
    "conversation_user_id"   VARCHAR(255),
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
    "id"                UUID NOT NULL,
    "application_id"    UUID,
    "conversation_id"   UUID,
    "workflow_run_id"   UUID,
    "type"              VARCHAR(20),
    "content"           JSONB,
    "context"           JSONB,
    "prompt_tokens"     INTEGER,
    "completion_tokens" INTEGER,
    "duration"          BIGINT,
    "create_time"       TIMESTAMP,
    "update_time"       TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "project"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "desc"        VARCHAR(256),
    "name"        VARCHAR(256),
    "icon"        VARCHAR(256),
    "path"        TEXT,
    "star"        BOOLEAN,
    "share"       BOOLEAN,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);


CREATE TABLE "project_folder"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);


CREATE TABLE "project_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "project_permission"
(
    "id"          UUID NOT NULL,
    "user_id"     UUID,
    "target"      UUID,
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "processor"
(
    "id"          UUID NOT NULL,
    "project_id"  UUID NOT NULL,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "protocol"    VARCHAR(32),
    "meta"        JSONB,
    "workflow"    JSONB,
    "activate"    BOOLEAN,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "role"
(
    "id"          VARCHAR(32) NOT NULL,
    "name"        VARCHAR(255),
    "internal"    BOOLEAN,
    "type"        VARCHAR(64),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "role_permission_relation"
(
    "id"            UUID NOT NULL,
    "role_id"       VARCHAR(32),
    "permission_id" VARCHAR(255),
    PRIMARY KEY ("id")
);

CREATE TABLE "role_user_relation"
(
    "id"      UUID NOT NULL,
    "role_id" VARCHAR(32),
    "user_id" UUID,
    PRIMARY KEY ("id")
);

INSERT INTO "role" ("id", "name", "internal", "type", "create_time", "update_time")
VALUES ('ADMIN', 'ADMIN', TRUE, 'ADMIN', '2026-04-16 23:00:01', '2026-04-16 23:00:01'),
       ('USER', 'USER', TRUE, 'USER', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO "role_user_relation" ("id", "role_id", "user_id")
VALUES ('a0b1c2d3-e4f5-6789-abcd-ef0123456789', 'ADMIN', '22d90f6c-2092-43b8-aa14-d1f9731522ac');

CREATE TABLE "datasource"
(
    "id"               UUID NOT NULL,
    "parent_id"        UUID,
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
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "datasource_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "datasource_permission"
(
    "id"          UUID NOT NULL,
    "user_id"     UUID,
    "target"      UUID,
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
