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
  "role" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
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

COMMENT ON COLUMN "public"."user"."role" IS '角色';

INSERT INTO "public"."user" ("id", "email", "phone", "nick_name", "username", "password", "create_time", "update_time", "icon", "role") VALUES ('22d90f6c-2092-43b8-aa14-d1f9731522ac', '', NULL, '管理员', 'admin', '8ded6cfcf8a627e51d361fbccc4af20242bc2cd3239a6fafcc1b9b5eb0ffcb1d', '2022-04-17 00:59:01', '2025-04-05 00:00:00', '/ui/user.jpeg', 'ADMIN');

CREATE TABLE "public"."file" (
  "id" uuid NOT NULL,
  "file_name" varchar(255) COLLATE "pg_catalog"."default",
  "lo_id" int8,
  "sha256_hash" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "ref_type" varchar(255) COLLATE "pg_catalog"."default",
  "ref" varchar(255) COLLATE "pg_catalog"."default",
  "meta" jsonb,
  "create_time" timestamp(6),
  "update_time" timestamp(6),
  "size" int8,
  "path" varchar(255) COLLATE "pg_catalog"."default",
  CONSTRAINT "file_pkey" PRIMARY KEY ("id")
)
;

ALTER TABLE "public"."file"
  OWNER TO "postgres";


CREATE TABLE "public"."knowledge" (
  "id" uuid NOT NULL,
  "parent_id" uuid,
  "meta" jsonb,
  "create_time" timestamp(6),
  "update_time" timestamp(6),
  "name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "excerpt" varchar(255) COLLATE "pg_catalog"."default",
  "star" bool,
  "share" bool,
  "content" varchar(102400) COLLATE "pg_catalog"."default",
  CONSTRAINT "node_copy1_pkey" PRIMARY KEY ("id")
)
;

ALTER TABLE "public"."knowledge"
  OWNER TO "postgres";

COMMENT ON COLUMN "public"."knowledge"."parent_id" IS '父id';

COMMENT ON COLUMN "public"."knowledge"."type" IS '节点类型';

COMMENT ON COLUMN "public"."knowledge"."meta" IS '元数据';

COMMENT ON COLUMN "public"."knowledge"."create_time" IS '创建时间';

COMMENT ON COLUMN "public"."knowledge"."update_time" IS '修改时间';

COMMENT ON COLUMN "public"."knowledge"."name" IS '节点名称';

COMMENT ON COLUMN "public"."knowledge"."excerpt" IS '摘要';

COMMENT ON COLUMN "public"."knowledge"."star" IS '加星';

COMMENT ON COLUMN "public"."knowledge"."share" IS '分享';

COMMENT ON COLUMN "public"."knowledge"."content" IS '内容';


CREATE TABLE "public"."knowledge_relation" (
  "id" uuid NOT NULL,
  "ancestor_id" uuid,
  "descendant_id" uuid NOT NULL,
  "depth" int8 NOT NULL
)
;

ALTER TABLE "public"."knowledge_relation"
  OWNER TO "postgres";

COMMENT ON COLUMN "public"."knowledge_relation"."id" IS '闭包表id';

COMMENT ON COLUMN "public"."knowledge_relation"."ancestor_id" IS '祖先id';

COMMENT ON COLUMN "public"."knowledge_relation"."descendant_id" IS '后代id';

COMMENT ON COLUMN "public"."knowledge_relation"."depth" IS '层级';


CREATE TABLE "public"."application" (
  "id" uuid NOT NULL,
  "parent_id" uuid,
  "setting" jsonb,
  "create_time" timestamp(6),
  "update_time" timestamp(6),
  "name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "desc" varchar(255) COLLATE "pg_catalog"."default",
  "star" bool,
  "share" bool,
  "content" varchar(102400) COLLATE "pg_catalog"."default",
  "workflow" jsonb,
  CONSTRAINT "knowledge_copy1_pkey" PRIMARY KEY ("id")
)
;

ALTER TABLE "public"."application"
  OWNER TO "postgres";

COMMENT ON COLUMN "public"."application"."parent_id" IS '父id';

COMMENT ON COLUMN "public"."application"."type" IS '节点类型';

COMMENT ON COLUMN "public"."application"."setting" IS '设置';

COMMENT ON COLUMN "public"."application"."create_time" IS '创建时间';

COMMENT ON COLUMN "public"."application"."update_time" IS '修改时间';

COMMENT ON COLUMN "public"."application"."name" IS '节点名称';

COMMENT ON COLUMN "public"."application"."desc" IS '描述';

COMMENT ON COLUMN "public"."application"."star" IS '加星';

COMMENT ON COLUMN "public"."application"."share" IS '分享';

COMMENT ON COLUMN "public"."application"."content" IS '内容';

COMMENT ON COLUMN "public"."application"."workflow" IS '工作流对象';

CREATE TABLE "public"."application_relation" (
  "id" uuid NOT NULL,
  "ancestor_id" uuid,
  "descendant_id" uuid NOT NULL,
  "depth" int8 NOT NULL
)
;

ALTER TABLE "public"."application_relation"
  OWNER TO "postgres";

COMMENT ON COLUMN "public"."application_relation"."id" IS '闭包表id';

COMMENT ON COLUMN "public"."application_relation"."ancestor_id" IS '祖先id';

COMMENT ON COLUMN "public"."application_relation"."descendant_id" IS '后代id';

COMMENT ON COLUMN "public"."application_relation"."depth" IS '层级';