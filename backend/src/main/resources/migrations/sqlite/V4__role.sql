CREATE TABLE "role" (
    "id" text(32) NOT NULL,
    "name" text(255),
    "internal" INTEGER,
    "type" text(64),
    "create_time" TIMESTAMP,
    "update_time" TIMESTAMP,
    PRIMARY KEY ("id" DESC)
);

CREATE TABLE "role_permission_relation" (
    "id" text(32) NOT NULL,
    "role_id" text(32),
    "permission_id" text(255),
    PRIMARY KEY ("id" DESC)
);

CREATE TABLE "role_user_relation" (
    "id" text(32) NOT NULL,
    "role_id" text(32),
    "user_id" text(32),
    PRIMARY KEY ("id" DESC)
);


INSERT INTO "role" (
    "id",
    "name",
    "internal",
    "type",
    "create_time",
    "update_time"
) VALUES
    ('ADMIN', 'ADMIN', 1, 'ADMIN', '2026-04-16 23:00:01', '2026-04-16 23:00:01'),
    ('USER', 'USER', 1, 'USER', '2026-04-16 23:00:01', '2026-04-16 23:00:01');