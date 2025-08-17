CREATE TABLE "conversation" (
  "id" text NOT NULL,
  "name" text(256),
  "application_id" text,
  "is_deleted" integer(2),
  "meta" TEXT,
  "conversation_user_id" text,
  "conversation_user_type" TEXT,
  "star_num" INTEGER,
  "trample_num" INTEGER,
  "mark_sum" INTEGER,
  "conversation_record_count" INTEGER,
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "conversation_record" (
  "id" text(128) NOT NULL,
  "application_id" text(128),
  "conversation_id" text(128),
  "star" integer(2),
  "trample" integer(2),
  "question" text,
  "answer" TEXT,
  "details" TEXT,
  "run_time" real,
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);