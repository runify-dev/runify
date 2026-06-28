CREATE TABLE "integration"
(
    "id"             UUID NOT NULL,
    "parent_id"      UUID,
    "name"           VARCHAR(256),
    "icon"           VARCHAR(256),
    "desc"           VARCHAR(256),
    "type"           VARCHAR(64),
    "application_id" UUID,
    "config"         TEXT,
    "enabled"        BOOLEAN,
    "meta"           JSONB,
    "create_time"    TIMESTAMP,
    "update_time"    TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "integration_folder"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "integration_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

CREATE TABLE "integration_permission"
(
    "id"          UUID NOT NULL,
    "user_id"     UUID,
    "target"      UUID,
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

INSERT INTO integration_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', NOW(), NOW());

INSERT INTO integration_relation (id, ancestor_id, descendant_id, depth)
VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);
