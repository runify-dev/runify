CREATE TABLE "knowledge" (
                             "id" text(32) NOT NULL,
                             "parent_id" text(32),
                             "name" TEXT(256),
                             "type" TEXT(32),
                             "content" TEXT,
                             "excerpt" TEXT(256),
                             "star" INTEGER(2),
                             "share" INTEGER(2),
                             "meta" TEXT,
                             "create_time" TIMESTAMP,
                             "update_time" TIMESTAMP,
                             PRIMARY KEY ("id")
);