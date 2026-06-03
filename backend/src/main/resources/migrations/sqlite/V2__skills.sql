CREATE TABLE "skill"
(
    "id"                    text NOT NULL,
    "parent_id"             text,
    "name"                  text,
    "icon"                  text,
    "desc"                  text,
    "parameter_value"       text,
    "skill_parameter_form"  text,
    "create_time"           TIMESTAMP,
    "update_time"           TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_folder"
(
    "id"          text NOT NULL,
    "parent_id"   text,
    "name"        text,
    "desc"        text,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_relation"
(
    "id"            text NOT NULL,
    "ancestor_id"   text,
    "descendant_id" text,
    "depth"         integer,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_permission"
(
    "id"          text NOT NULL,
    "user_id"     text,
    "target"      TEXT,
    "permission"  TEXT,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE TABLE "skill_file"
(
    "id"          text NOT NULL,
    "parent_id"   text,
    "skill_id"    text NOT NULL,
    "name"        text,
    "type"        text,
    "content"     TEXT,
    "file_id"     text,
    "file_name"   text,
    "file_size"   integer,
    "desc"        text,
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id")
);

INSERT INTO skill_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT INTO skill_relation (id, ancestor_id, descendant_id, depth)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);
