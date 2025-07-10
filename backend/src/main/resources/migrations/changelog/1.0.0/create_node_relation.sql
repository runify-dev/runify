CREATE TABLE "public"."node_relation" (
                                          "id" uuid NOT NULL,
                                          "ancestor_id" uuid,
                                          "descendant_id" uuid NOT NULL,
                                          "depth" int8 NOT NULL,
                                          "type" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                          "source" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                          "name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL
)
;

ALTER TABLE "public"."node_relation"
    OWNER TO "postgres";

COMMENT ON COLUMN "public"."node_relation"."id" IS '闭包表id';

COMMENT ON COLUMN "public"."node_relation"."ancestor_id" IS '祖先id';

COMMENT ON COLUMN "public"."node_relation"."descendant_id" IS '后代id';

COMMENT ON COLUMN "public"."node_relation"."depth" IS '层级';

COMMENT ON COLUMN "public"."node_relation"."type" IS '后代节点类型';

COMMENT ON COLUMN "public"."node_relation"."source" IS '节点source';

COMMENT ON COLUMN "public"."node_relation"."name" IS '后代节点名称';