CREATE TABLE "file" (
                        "id" text(32) NOT NULL,
                        "file_name" text(255),
                        "lo_id" INTEGER,
                        "sha256_hash" text(255),
                        "ref_type" text(64),
                        "ref" TEXT(32),
                        "meta" TEXT,
                        "create_time" TIMESTAMP,
                        "update_time" TIMESTAMP,
                        "path" TEXT,
                        "size" INTEGER(32),
                        PRIMARY KEY ("id" DESC)
);