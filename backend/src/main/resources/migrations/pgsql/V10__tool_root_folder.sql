-- 工具根文件夹 + 自关联（幂等：已存在则跳过，兼容 V9 已建表的库）
INSERT INTO tool_folder (id, parent_id, name, "desc", create_time, update_time)
SELECT '00000000-0000-0000-0000-000000000000', NULL, 'root', '', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tool_folder WHERE id = '00000000-0000-0000-0000-000000000000');

INSERT INTO tool_relation (id, ancestor_id, descendant_id, depth)
SELECT gen_random_uuid(), '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0
WHERE NOT EXISTS (SELECT 1 FROM tool_relation
                  WHERE ancestor_id = '00000000-0000-0000-0000-000000000000'
                    AND descendant_id = '00000000-0000-0000-0000-000000000000');
