-- 存量处理器各生成一条初始版本 v1(snapshot = {"workflow","meta"}),部署读已发布版本后不至于失效。
-- 独立于 V12:V12 已在既有库应用,不可再改其内容(否则 Flyway checksum 不匹配)。
INSERT INTO "resource_version" ("id", "resource_type", "resource_id", "version", "snapshot", "remark", "create_user", "create_time", "update_time")
SELECT gen_random_uuid(), 'processor', p."id", 1,
       jsonb_build_object('workflow', COALESCE(p."workflow", '{}'::jsonb), 'meta', COALESCE(p."meta", '{}'::jsonb)),
       '初始版本', NULL, NOW(), NOW()
FROM "processor" p
WHERE NOT EXISTS (SELECT 1 FROM "resource_version" rv
                  WHERE rv."resource_type" = 'processor' AND rv."resource_id" = p."id");
