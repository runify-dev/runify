CREATE TABLE "knowledge"
(
    "id"          VARCHAR(36) NOT NULL,
    "parent_id"   VARCHAR(36),
    "name"        VARCHAR(256),
    "icon"        VARCHAR(256),
    "desc"        VARCHAR(512),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "knowledge_folder"
(
    "id"          VARCHAR(36) NOT NULL,
    "parent_id"   VARCHAR(36),
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "knowledge_relation"
(
    "id"            VARCHAR(36) NOT NULL,
    "ancestor_id"   VARCHAR(36),
    "descendant_id" VARCHAR(36),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

INSERT INTO knowledge_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', datetime('now'), datetime('now'));

INSERT INTO knowledge_relation (id, ancestor_id, descendant_id, depth)
VALUES (hex(randomblob(16)), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);

CREATE TABLE "knowledge_permission"
(
    "id"          VARCHAR(36) NOT NULL,
    "user_id"     VARCHAR(36),
    "target"      VARCHAR(36),
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "document"
(
    "id"           VARCHAR(36) NOT NULL,
    "parent_id"    VARCHAR(36),
    "knowledge_id" VARCHAR(36) NOT NULL,
    "name"         VARCHAR(256),
    "icon"         VARCHAR(256),
    "content"      TEXT,
    "excerpt"      VARCHAR(512),
    "create_time"  TIMESTAMP,
    "update_time"  TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "document_folder"
(
    "id"           VARCHAR(36) NOT NULL,
    "parent_id"    VARCHAR(36),
    "knowledge_id" VARCHAR(36) NOT NULL,
    "name"         VARCHAR(256),
    "desc"         VARCHAR(256),
    "create_time"  TIMESTAMP,
    "update_time"  TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "document_relation"
(
    "id"            VARCHAR(36) NOT NULL,
    "ancestor_id"   VARCHAR(36),
    "descendant_id" VARCHAR(36),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

INSERT INTO document_folder (id, parent_id, knowledge_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, '00000000-0000-0000-0000-000000000000', 'root', '', datetime('now'), datetime('now'));

INSERT INTO document_relation (id, ancestor_id, descendant_id, depth)
VALUES (hex(randomblob(16)), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);
