CREATE TABLE "skill"
(
    "id"                    UUID NOT NULL,
    "parent_id"             UUID,
    "name"                  VARCHAR(256),
    "icon"                  VARCHAR(256),
    "desc"                  VARCHAR(512),
    "parameter_value"       TEXT,
    "skill_parameter_form"  JSONB,
    "create_time"           TIMESTAMP,
    "update_time"           TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_folder"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "name"        VARCHAR(256),
    "desc"        VARCHAR(256),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_relation"
(
    "id"            UUID NOT NULL,
    "ancestor_id"   UUID,
    "descendant_id" UUID,
    "depth"         INTEGER,
    PRIMARY KEY ("id")
);

INSERT INTO skill_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', NOW(), NOW());

INSERT INTO skill_relation (id, ancestor_id, descendant_id, depth)
VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);

CREATE TABLE "skill_permission"
(
    "id"          UUID NOT NULL,
    "user_id"     UUID,
    "target"      UUID,
    "permission"  VARCHAR(255),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_file"
(
    "id"          UUID NOT NULL,
    "parent_id"   UUID,
    "skill_id"    UUID NOT NULL,
    "name"        VARCHAR(256),
    "type"        VARCHAR(32),
    "content"     TEXT,
    "file_id"     UUID,
    "file_name"   VARCHAR(512),
    "file_size"   BIGINT,
    "desc"        VARCHAR(512),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);
