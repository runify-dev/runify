CREATE TABLE "public"."user" (
                                 "id" uuid NOT NULL,
                                 "email" varchar(255) COLLATE "pg_catalog"."default",
                                 "phone" varchar(255) COLLATE "pg_catalog"."default",
                                 "nick_name" varchar(255) COLLATE "pg_catalog"."default",
                                 "username" varchar(255) COLLATE "pg_catalog"."default",
                                 "password" varchar(255) COLLATE "pg_catalog"."default",
                                 "create_time" timestamp(6),
                                 "update_time" timestamp(6),
                                 "icon" varchar(255) COLLATE "pg_catalog"."default",
                                 CONSTRAINT "user_pkey" PRIMARY KEY ("id")
)
;

ALTER TABLE "public"."user"
    OWNER TO "postgres";

COMMENT ON COLUMN "public"."user"."id" IS '用户id';

COMMENT ON COLUMN "public"."user"."email" IS '邮箱';

COMMENT ON COLUMN "public"."user"."phone" IS '手机号';

COMMENT ON COLUMN "public"."user"."nick_name" IS '昵称';

COMMENT ON COLUMN "public"."user"."username" IS '用户名';

COMMENT ON COLUMN "public"."user"."password" IS '密码';

COMMENT ON COLUMN "public"."user"."create_time" IS '创建时间';

COMMENT ON COLUMN "public"."user"."update_time" IS '修改时间';

COMMENT ON COLUMN "public"."user"."icon" IS '用户图标';

INSERT INTO "public"."user" ("id", "email", "phone", "nick_name", "username", "password", "create_time", "update_time", "icon") VALUES ('22d90f6c-2092-43b8-aa14-d1f9731522ac', 'shaohuzhang1@163.com', NULL, '管理员', 'admin', '1eade54e54a4611afd40a4c9a21c25d2eb2bbd33b6fb0bd5786bae71240cb008', '2022-04-17 00:59:01', '2025-04-05 00:00:00', '/ui/user.jpeg');