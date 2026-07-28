-- 工具根文件夹 + 自关联（幂等：主键冲突则忽略，兼容 V9 已建表的库）
INSERT OR IGNORE INTO tool_folder (id, parent_id, name, "desc", create_time, update_time)
VALUES ('00000000-0000-0000-0000-000000000000', NULL, 'root', '', '2026-04-16 23:00:01', '2026-04-16 23:00:01');

INSERT OR IGNORE INTO tool_relation (id, ancestor_id, descendant_id, depth)
VALUES ('e7c1a2b3-4d5e-4f60-9a7b-8c9d0e1f2a3b', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 0);
