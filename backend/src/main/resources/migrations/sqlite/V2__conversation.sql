CREATE TABLE "conversation" (
  "id" text NOT NULL,
  "name" text(256),
  "application_id" text,
  "is_deleted" integer(2),
  "meta" TEXT,
  "conversation_user_id" text,
  "conversation_user_type" TEXT,
  "execute_type" TEXT,
  "star_num" INTEGER,
  "trample_num" INTEGER,
  "mark_sum" INTEGER,
  "conversation_count" INTEGER,
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "conversation_message" (
  "id" text(128) NOT NULL,
  "application_id" text(128),
  "conversation_id" text(128),
  "workflow_run_id" text(128),
  "type" text(20),
  "content" TEXT,
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);