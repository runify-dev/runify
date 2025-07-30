CREATE TABLE "user" (
	id UUID NOT NULL,
	email VARCHAR(255),
	phone VARCHAR(255),
	nick_name VARCHAR(255),
	username VARCHAR(255),
	password VARCHAR(255),
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	icon VARCHAR(255),
	CONSTRAINT USER_PK PRIMARY KEY (id)
);


INSERT INTO "user" ("id", "email", "phone", "nick_name", "username", "password", "create_time", "update_time", "icon") VALUES ('22d90f6c-2092-43b8-aa14-d1f9731522ac', 'shaohuzhang1@163.com', NULL, '管理员', 'admin', '32d991775d14e9fa31a3633eb3cd253d5c1ecfae8b64dc6d7391a29ccc6fd824', '2022-04-17 00:59:01', '2025-04-05 00:00:00', '/ui/user.jpeg');

CREATE TABLE file (
	id UUID NOT NULL,
	file_name VARCHAR(255),
	lo_id INTEGER,
	sha256_hash VARCHAR(255),
	ref_type VARCHAR(100),
	"ref" VARCHAR(255),
	meta VARCHAR,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	"size" INTEGER,
	"path" VARCHAR(255),
	CONSTRAINT FILE_PK PRIMARY KEY (id)
);


CREATE TABLE knowledge (
	id UUID,
	parent_id UUID,
	"type" VARCHAR(255),
	meta VARCHAR,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	name VARCHAR(255),
	excerpt VARCHAR(255),
	star BOOLEAN,
	share BOOLEAN,
	content VARCHAR,
	CONSTRAINT KNOWLEDGE_PK PRIMARY KEY (id)
);

CREATE TABLE knowledge_relation (
	id UUID,
	ancestor_id UUID,
	descendant_id UUID,
	"depth" INTEGER,
	CONSTRAINT KNOWLEDGE_RELATION_PK PRIMARY KEY (id)
);

CREATE TABLE application (
	id UUID,
	parent_id UUID,
	"type" VARCHAR(255),
	setting VARCHAR,
	create_time TIMESTAMP,
	update_time TIMESTAMP,
	name VARCHAR(255),
	"desc" VARCHAR(255),
	star BOOLEAN,
	share BOOLEAN,
	workflow VARCHAR,
	CONSTRAINT APPLICATION_PK PRIMARY KEY (id)
);


CREATE TABLE application_relation (
	id UUID,
	ancestor_id UUID,
	descendant_id UUID,
	"depth" INTEGER,
	CONSTRAINT APPLICATION_RELATION_PK PRIMARY KEY (id)
);