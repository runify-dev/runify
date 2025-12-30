CREATE TABLE "project" (
                             "id" text(32) NOT NULL,
                             "parent_id" text(32),
                             "desc" TEXT(256),
                             "name" TEXT(256),
                             "icon" TEXT(256),
                             "path" TEXT,
                             "star" INTEGER(2),
                             "share" INTEGER(2),
                             "create_time" TIMESTAMP,
                             "update_time" TIMESTAMP,
                             PRIMARY KEY ("id")
);


CREATE TABLE "project_folder" (
  "id" TEXT(32) NOT NULL,
  "parent_id" TEXT(32),
  "name" TEXT(256),
  "desc" TEXT(256),
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);


CREATE TABLE "project_relation" (
                                        "id" text(32) NOT NULL,
                                        "ancestor_id" text(32),
                                        "descendant_id" text(32),
                                        "depth" integer(10),
                                        PRIMARY KEY ("id")
);

CREATE TABLE "project_permission" (
  "id" text NOT NULL,
  "user_id" text,
  "target" TEXT,
  "permission" TEXT,
  "create_time" TIMESTAMP,
  "update_time" TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE TABLE "processor" (
                                        "id" text(32) NOT NULL,
                                        "project_id" text(32) NOT NULL,
                                        "name" TEXT(256),
                                        "desc" TEXT(256),
                                        "protocol" TEXT(32),
                                        "meta" TEXT,
                                        "workflow" TEXT,
                                        "activate" INTEGER(2),
                                        "create_time" TIMESTAMP,
                                        "update_time" TIMESTAMP,
                                        PRIMARY KEY ("id")
);