CREATE TABLE "integration"
(
    "id"             TEXT(32) NOT NULL,
    "parent_id"      TEXT(32),
    "name"           TEXT(256),
    "icon"           TEXT(256),
    "desc"           TEXT(256),
    "type"           TEXT(64),
    "application_id" TEXT(32),
    "config"         TEXT,
    "enabled"        integer(2),
    "meta"           TEXT,
    "create_time"    TIMESTAMP,
    "update_time"    TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "integration_folder"
(
    "id"          TEXT(32) NOT NULL,
    "parent_id"   TEXT(32),
    "name"        TEXT(256),
    "desc"        TEXT(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "integration_relation"
(
    "id"            text(32) NOT NULL,
    "ancestor_id"   text(32),
    "descendant_id" text(32),
    "depth"         integer(10),
    PRIMARY KEY ("id")
);

CREATE TABLE "integration_permission"
(
    "id"          TEXT(32) NOT NULL,
    "user_id"     TEXT(32),
    "target"      TEXT(32),
    "permission"  TEXT(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

INSERT INTO integration_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO integration_relation (id, ancestor_id, descendant_id, depth)
VALUES ('b2d9f7a1-3c4e-4f5a-8b6c-0d1e2f3a4b5c', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);
