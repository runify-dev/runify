CREATE TABLE "ctx_fact" (
                            "id" text(128) NOT NULL,
                            "scope_type" text(32),
                            "scope_id" text(256),
                            "subtype" text(64),
                            "fact_key" text(512),
                            "fact_value" TEXT,
                            "provisional" integer(2),
                            "create_time" TIMESTAMP,
                            "update_time" TIMESTAMP,
                            "application_id" text(256),
                            PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_ctx_fact_key"
    ON "ctx_fact" (
                   "scope_type",
                   "scope_id",
                   "application_id",
                   "subtype",
                   "fact_key"
        );

CREATE INDEX "idx_ctx_fact_gc"
    ON "ctx_fact" (
                   "scope_type",
                   "update_time"
        );


CREATE TABLE "ctx_summary" (
                               "id" text(128) NOT NULL,
                               "scope_type" text(32),
                               "scope_id" text(256),
                               "summary_text" TEXT,
                               "covered_upto" TIMESTAMP,
                               "create_time" TIMESTAMP,
                               "update_time" TIMESTAMP,
                               PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_ctx_summary_scope"
    ON "ctx_summary" (
                      "scope_type",
                      "scope_id"
        );


CREATE TABLE "ctx_section" (
                               "id" text(128) NOT NULL,
                               "application_id" text(128),
                               "section_key" text(64),
                               "label" text(128),
                               "description" TEXT,
                               "scope" text(32),
                               "list_style" integer(2),
                               "enabled" integer(2),
                               "sort_order" INTEGER,
                               "create_time" TIMESTAMP,
                               "update_time" TIMESTAMP,
                               PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_ctx_section"
    ON "ctx_section" (
                      "application_id",
                      "section_key"
        );