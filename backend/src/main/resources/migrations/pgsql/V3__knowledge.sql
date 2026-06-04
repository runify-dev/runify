CREATE TABLE "knowledge"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "icon"        VARCHAR(256),
    "desc"        VARCHAR(512),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "knowledge_folder"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "knowledge_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

INSERT INTO knowledge_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', NOW(), NOW());

INSERT INTO knowledge_relation (id, ancestor_id, descendant_id, depth)
VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);

CREATE TABLE "knowledge_permission"
(
    "id"          UUID NOT NULL,
    "user_id"     UUID,
    "target"      UUID,
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "document"
(
    "id"           UUID NOT NULL,
    "parent_id"    UUID,
    "knowledge_id" UUID NOT NULL,
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
    "id"           UUID NOT NULL,
    "parent_id"    UUID,
    "knowledge_id" UUID NOT NULL,
    "name"         VARCHAR(256),
    "desc"         VARCHAR(256),
    "create_time"  TIMESTAMP,
    "update_time"  TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "document_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

INSERT INTO document_folder (id, parent_id, knowledge_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, '00000000-0000-0000-0000-000000000000', 'root', '', NOW(), NOW());

INSERT INTO document_relation (id, ancestor_id, descendant_id, depth)
VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);
