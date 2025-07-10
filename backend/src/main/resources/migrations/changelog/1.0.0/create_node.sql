CREATE TABLE "public"."node" (
                                 "id" uuid NOT NULL,
                                 "parent_id" uuid,
                                 "type" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                 "source" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                 "meta" jsonb,
                                 "create_time" timestamp(6),
                                 "update_time" timestamp(6),
                                 "name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                 "subtype" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                 "excerpt" varchar(255) COLLATE "pg_catalog"."default",
                                 "star" bool,
                                 "share" bool,
                                 CONSTRAINT "node_pkey" PRIMARY KEY ("id")
)
;

ALTER TABLE "public"."node"
    OWNER TO "postgres";

COMMENT ON COLUMN "public"."node"."parent_id" IS '父id';

COMMENT ON COLUMN "public"."node"."type" IS '节点类型';

COMMENT ON COLUMN "public"."node"."source" IS '所属';

COMMENT ON COLUMN "public"."node"."meta" IS '元数据';

COMMENT ON COLUMN "public"."node"."create_time" IS '创建时间';

COMMENT ON COLUMN "public"."node"."update_time" IS '修改时间';

COMMENT ON COLUMN "public"."node"."name" IS '节点名称';

COMMENT ON COLUMN "public"."node"."subtype" IS '子类型';

COMMENT ON COLUMN "public"."node"."excerpt" IS '摘要';

COMMENT ON COLUMN "public"."node"."star" IS '加星';

COMMENT ON COLUMN "public"."node"."share" IS '分享';