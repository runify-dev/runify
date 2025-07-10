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
                                 CONSTRAINT "file_pkey" PRIMARY KEY ("id")
)
;

ALTER TABLE "public"."file"
    OWNER TO "postgres";