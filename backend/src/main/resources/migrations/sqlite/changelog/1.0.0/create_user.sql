CREATE TABLE "user" (
                        "id" TEXT NOT NULL,
                        "email" TEXT NOT NULL,
                        "phone" TEXT,
                        "nick_name" TEXT NOT NULL,
                        "username" TEXT NOT NULL,
                        "icon" TEXT,
                        "password" TEXT NOT NULL,
                        "create_time" TIMESTAMP NOT NULL,
                        "update_time" TIMESTAMP NOT NULL,
                        PRIMARY KEY ("id")
);

INSERT INTO "user" ("id", "email", "phone", "nick_name", "username", "password", "create_time", "update_time", "icon") VALUES ('22d90f6c-2092-43b8-aa14-d1f9731522ac', 'shaohuzhang1@163.com', NULL, '管理员', 'admin', '1eade54e54a4611afd40a4c9a21c25d2eb2bbd33b6fb0bd5786bae71240cb008', '2022-04-17 00:59:01', '2025-04-05 00:00:00', '/ui/user.jpeg');