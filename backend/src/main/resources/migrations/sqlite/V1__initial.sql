CREATE TABLE "user"
(
    "id"          TEXT      NOT NULL,
    "email"       TEXT      NOT NULL,
    "phone"       TEXT,
    "nick_name"   TEXT      NOT NULL,
    "username"    TEXT      NOT NULL,
    "icon"        TEXT,
    "role"        TEXT      NOT NULL,
    "password"    TEXT      NOT NULL,
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
    "id"          text(32) NOT NULL,
    "file_name"   text(255),
    "lo_id"       INTEGER,
    "sha256_hash" text(255),
    "ref_type"    text(64),
    "ref"         TEXT(32),
    "meta"        TEXT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    "path"        TEXT,
    "size"        INTEGER(32),
    PRIMARY KEY ("id" DESC)
);


CREATE TABLE "application_relation"
(
    "id"            text(32) NOT NULL,
    "ancestor_id"   text(32),
    "descendant_id" text(32),
    "depth"         integer(10),
    PRIMARY KEY ("id")
);

CREATE TABLE "application"
(
    "id"                     TEXT(32) NOT NULL,
    "parent_id"              TEXT(32),
    "name"                   TEXT(256),
    "icon"                   TEXT(256),
    "desc"                   TEXT(256),
    "workflow"               TEXT,
    "setting"                TEXT,
    "star"                   integer(2),
    "share"                  integer(2),
    "allow_anonymous_access" integer(2),
    "create_time"            TIMESTAMP,
    "update_time"            TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "application_folder"
(
    "id"          TEXT(32) NOT NULL,
    "parent_id"   TEXT(32),
    "name"        TEXT(256),
    "desc"        TEXT(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "application_permission"
(
    "id"          text NOT NULL,
    "user_id"     text,
    "target"      TEXT,
    "permission"  TEXT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "model"
(
    "id"                   TEXT(32) NOT NULL,
    "parent_id"            TEXT(32),
    "name"                 TEXT(256),
    "icon"                 TEXT(256),
    "desc"                 TEXT(256),
    "provider"             TEXT(128),
    "model_type"           TEXT(256),
    "model_name"           TEXT(256),
    "credential"           TEXT,
    "model_parameter_form" TEXT,
    "meta"                 TEXT,
    "star"                 integer(2),
    "share"                integer(2),
    "create_time"          TIMESTAMP,
    "update_time"          TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "model_folder"
(
    "id"          TEXT(32) NOT NULL,
    "parent_id"   TEXT(32),
    "name"        TEXT(256),
    "desc"        TEXT(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "model_relation"
(
    "id"            text(32) NOT NULL,
    "ancestor_id"   text(32),
    "descendant_id" text(32),
    "depth"         integer(10),
    PRIMARY KEY ("id")
);

CREATE TABLE "model_permission"
(
    "id"          text NOT NULL,
    "user_id"     text,
    "target"      TEXT,
    "permission"  TEXT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);


CREATE TABLE "system_setting"
(
    "type" TEXT NOT NULL,
    "meta" TEXT,
    PRIMARY KEY ("type")
);


CREATE TABLE "note"
(
    "id"          text(32) NOT NULL,
    "parent_id"   text(32),
    "name"        TEXT(256),
    "icon"        TEXT(256),
    "content"     TEXT,
    "excerpt"     TEXT(256),
    "star"        INTEGER(2),
    "share"       INTEGER(2),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "note_folder"
(
    "id"          TEXT(32) NOT NULL,
    "parent_id"   TEXT(32),
    "name"        TEXT(256),
    "desc"        TEXT(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "note_relation"
(
    "id"            text(32) NOT NULL,
    "ancestor_id"   text(32),
    "descendant_id" text(32),
    "depth"         integer(10),
    PRIMARY KEY ("id")
);
CREATE TABLE "note_permission"
(
    "id"          text NOT NULL,
    "user_id"     text,
    "target"      TEXT,
    "permission"  TEXT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "conversation"
(
    "id"                     text NOT NULL,
    "name"                   text(256),
    "application_id"         text,
    "is_deleted"             integer(2),
    "meta"                   TEXT,
    "conversation_user_id"   text,
    "conversation_user_type" TEXT,
    "execute_type"           TEXT,
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
    "id"                text(128) NOT NULL,
    "application_id"    text(128),
    "conversation_id"   text(128),
    "workflow_run_id"   text(128),
    "type"              text(20),
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
    "id"          text(32) NOT NULL,
    "parent_id"   text(32),
    "desc"        TEXT(256),
    "name"        TEXT(256),
    "icon"        TEXT(256),
    "path"        TEXT,
    "star"        INTEGER(2),
    "share"       INTEGER(2),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);


CREATE TABLE "project_folder"
(
    "id"          TEXT(32) NOT NULL,
    "parent_id"   TEXT(32),
    "name"        TEXT(256),
    "desc"        TEXT(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);


CREATE TABLE "project_relation"
(
    "id"            text(32) NOT NULL,
    "ancestor_id"   text(32),
    "descendant_id" text(32),
    "depth"         integer(10),
    PRIMARY KEY ("id")
);

CREATE TABLE "project_permission"
(
    "id"          text NOT NULL,
    "user_id"     text,
    "target"      TEXT,
    "permission"  TEXT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "processor"
(
    "id"          text(32) NOT NULL,
    "project_id"  text(32) NOT NULL,
    "name"        TEXT(256),
    "desc"        TEXT(256),
    "protocol"    TEXT(32),
    "meta"        TEXT,
    "workflow"    TEXT,
    "activate"    INTEGER(2),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "role"
(
    "id"          text(32) NOT NULL,
    "name"        text(255),
    "internal"    INTEGER,
    "type"        text(64),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id" DESC)
);

CREATE TABLE "role_permission_relation"
(
    "id"            text(32) NOT NULL,
    "role_id"       text(32),
    "permission_id" text(255),
    PRIMARY KEY ("id" DESC)
);

CREATE TABLE "role_user_relation"
(
    "id"      text(32) NOT NULL,
    "role_id" text(32),
    "user_id" text(32),
    PRIMARY KEY ("id" DESC)
);

INSERT INTO "role" ("id", "name", "internal", "type", "create_time", "update_time")
VALUES ('ADMIN', 'ADMIN', 1, 'ADMIN', '2026-04-16 23:00:01', '2026-04-16 23:00:01'),
       ('USER', 'USER', 1, 'USER', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO "role_user_relation" ("id", "role_id", "user_id")
VALUES ('27cbd856-df53-42a9-a6db-40cc8de4ff75', 'ADMIN', '22d90f6c-2092-43b8-aa14-d1f9731522ac');

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

INSERT INTO application_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO application_relation (id, ancestor_id, descendant_id, depth)
VALUES ('35f984af-7f7d-4cdb-b230-2dab798a70c9', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);

INSERT INTO model_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO model_relation (id, ancestor_id, descendant_id, depth)
VALUES ('f9c32425-d4f5-406a-a3ae-91ef290b00b9', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);

INSERT INTO note_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO note_relation (id, ancestor_id, descendant_id, depth)
VALUES ('10b206ac-eb1a-4346-9d89-1fe9d2e5c11e', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);

INSERT INTO project_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO project_relation (id, ancestor_id, descendant_id, depth)
VALUES ('aa7e413e-77cb-4b4b-b3c2-87be95ff0197', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);

INSERT INTO datasource_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO datasource_relation (id, ancestor_id, descendant_id, depth)
VALUES ('c10f604f-76f9-43d3-a6ed-4f2dbb77f3dd', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);
