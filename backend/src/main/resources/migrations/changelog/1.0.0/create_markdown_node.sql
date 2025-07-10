CREATE TABLE "public"."markdown_node" (
                                          "id" uuid NOT NULL,
                                          "content" varchar(102400) COLLATE "pg_catalog"."default",
                                          "create_time" timestamp(6),
                                          "update_time" timestamp(6),
                                          CONSTRAINT "markdown_node_pkey" PRIMARY KEY ("id")
)
;

ALTER TABLE "public"."markdown_node"
    OWNER TO "postgres";

COMMENT ON COLUMN "public"."markdown_node"."id" IS '节点id';

COMMENT ON COLUMN "public"."markdown_node"."content" IS '内容';

COMMENT ON COLUMN "public"."markdown_node"."create_time" IS '创建时间';

COMMENT ON COLUMN "public"."markdown_node"."update_time" IS '修改时间';