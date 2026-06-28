CREATE TABLE "integration"
(
    "id"             VARCHAR(32) NOT NULL,
    "parent_id"      VARCHAR(32),
    "name"           VARCHAR(256),
    "icon"           VARCHAR(256),
    "desc"           VARCHAR(256),
    "type"           VARCHAR(64),
    "application_id" VARCHAR(32),
    "config"         TEXT,
    "enabled"        SMALLINT,
    "meta"           TEXT,
    "create_time"    TIMESTAMP,
    "update_time"    TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "integration_folder"
(
    "id"          VARCHAR(32) NOT NULL,
    "parent_id"   VARCHAR(32),
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "integration_relation"
(
    "id"            VARCHAR(32) NOT NULL,
    "ancestor_id"   VARCHAR(32),
    "descendant_id" VARCHAR(32),
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "integration_permission"
(
    "id"          VARCHAR(32) NOT NULL,
    "user_id"     VARCHAR(32),
    "target"      VARCHAR(32),
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

INSERT INTO integration_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', NOW(), NOW());

INSERT INTO integration_relation (id, ancestor_id, descendant_id, depth)
VALUES (RANDOM_UUID(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);
