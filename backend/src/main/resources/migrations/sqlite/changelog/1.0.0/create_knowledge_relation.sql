CREATE TABLE "knowledge_relation" (
                                      "id" text(32) NOT NULL,
                                      "ancestor_id" text(32),
                                      "descendant_id" text(32),
                                      "depth" integer(10),
                                      PRIMARY KEY ("id")
);