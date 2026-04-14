CREATE TABLE "user" (
  "id" TEXT NOT NULL,
  "email" TEXT NOT NULL,
  "phone" TEXT,
  "nick_name" TEXT NOT NULL,
  "username" TEXT NOT NULL,
  "icon" TEXT,
  "role" TEXT NOT NULL,
  "password" TEXT NOT NULL,
  "create_time" TIMESTAMP NOT NULL,
  "update_time" TIMESTAMP NOT NULL,
  PRIMARY KEY ("id")
);

INSERT INTO "user" ("id", "email", "phone", "nick_name", "username","role", "password", "create_time", "update_time", "icon") VALUES ('22d90f6c-2092-43b8-aa14-d1f9731522ac', 'shaohuzhang1@163.com', NULL, '管理员', 'admin', 'ADMIN','32d991775d14e9fa31a3633eb3cd253d5c1ecfae8b64dc6d7391a29ccc6fd824', '2022-04-17 00:59:01', '2025-04-05 00:00:00', './user.jpeg');

CREATE TABLE "file" (
                        "id" text(32) NOT NULL,
                        "file_name" text(255),
                        "lo_id" INTEGER,
                        "sha256_hash" text(255),
                        "ref_type" text(64),
                        "ref" TEXT(32),
                        "meta" TEXT,
                        "create_time" TIMESTAMP,
                        "update_time" TIMESTAMP,
                        "path" TEXT,
                        "size" INTEGER(32),
                        PRIMARY KEY ("id" DESC)
);

CREATE TABLE "knowledge" (
                             "id" text(32) NOT NULL,
                             "parent_id" text(32),
                             "name" TEXT(256),
                             "icon" TEXT(256),
                             "content" TEXT,
                             "excerpt" TEXT(256),
                             "star" INTEGER(2),
                             "share" INTEGER(2),
                             "meta" TEXT,
                             "create_time" TIMESTAMP,
                             "update_time" TIMESTAMP,
                             PRIMARY KEY ("id")
);

CREATE TABLE "knowledge_folder" (
  "id" TEXT(32) NOT NULL,
  "parent_id" TEXT(32),
  "name" TEXT(256),
  "desc" TEXT(256),
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "knowledge_relation" (
                                      "id" text(32) NOT NULL,
                                      "ancestor_id" text(32),
                                      "descendant_id" text(32),
                                      "depth" integer(10),
                                      PRIMARY KEY ("id")
);
CREATE TABLE "knowledge_permission" (
  "id" text NOT NULL,
  "user_id" text,
  "target" TEXT,
  "permission" TEXT,
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "application_relation" (
                                        "id" text(32) NOT NULL,
                                        "ancestor_id" text(32),
                                        "descendant_id" text(32),
                                        "depth" integer(10),
                                        PRIMARY KEY ("id")
);

CREATE TABLE "application" (
                               "id" TEXT(32) NOT NULL,
                               "parent_id" TEXT(32),
                               "name" TEXT(256),
                               "icon" TEXT(256),
                               "desc" TEXT(256),
                               "workflow" TEXT,
                               "setting" TEXT,
                               "star" integer(2),
                               "share" integer(2),
                               "create_time" TIMESTAMP,
                               "update_time" TIMESTAMP,
                               PRIMARY KEY ("id")
);

CREATE TABLE "application_folder" (
  "id" TEXT(32) NOT NULL,
  "parent_id" TEXT(32),
  "name" TEXT(256),
  "desc" TEXT(256),
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "application_permission" (
  "id" text NOT NULL,
  "user_id" text,
  "target" TEXT,
  "permission" TEXT,
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "model" (
  "id" TEXT(32) NOT NULL,
  "parent_id" TEXT(32),
  "name" TEXT(256),
  "icon" TEXT(256),
  "desc" TEXT(256),
  "provider" TEXT(128),
  "model_type" TEXT(256),
  "model_name" TEXT(256),
  "credential" TEXT,
  "model_parameter_form" TEXT,
  "meta" TEXT,
  "star" integer(2),
  "share" integer(2),
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "model_folder" (
  "id" TEXT(32) NOT NULL,
  "parent_id" TEXT(32),
  "name" TEXT(256),
  "desc" TEXT(256),
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "model_relation" (
                                        "id" text(32) NOT NULL,
                                        "ancestor_id" text(32),
                                        "descendant_id" text(32),
                                        "depth" integer(10),
                                        PRIMARY KEY ("id")
);

CREATE TABLE "model_permission" (
  "id" text NOT NULL,
  "user_id" text,
  "target" TEXT,
  "permission" TEXT,
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);


CREATE TABLE "system_setting" (
  "type" TEXT NOT NULL,
  "meta" TEXT,
  PRIMARY KEY ("type")
);


CREATE TABLE "note" (
                             "id" text(32) NOT NULL,
                             "parent_id" text(32),
                             "name" TEXT(256),
                             "icon" TEXT(256),
                             "content" TEXT,
                             "excerpt" TEXT(256),
                             "star" INTEGER(2),
                             "share" INTEGER(2),
                             "create_time" TIMESTAMP,
                             "update_time" TIMESTAMP,
                             PRIMARY KEY ("id")
);

CREATE TABLE "note_folder" (
  "id" TEXT(32) NOT NULL,
  "parent_id" TEXT(32),
  "name" TEXT(256),
  "desc" TEXT(256),
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "note_relation" (
                                      "id" text(32) NOT NULL,
                                      "ancestor_id" text(32),
                                      "descendant_id" text(32),
                                      "depth" integer(10),
                                      PRIMARY KEY ("id")
);
CREATE TABLE "note_permission" (
  "id" text NOT NULL,
  "user_id" text,
  "target" TEXT,
  "permission" TEXT,
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);