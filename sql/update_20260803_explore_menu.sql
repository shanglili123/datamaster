-- 数据探查菜单重构
-- 1. 一级菜单"元数据管理"改名为"数据探查"
-- 2. 隐藏旧的"质量探查任务"菜单(2752) 和"数据探查"子目录(2555)
-- 3. 新增"探查结果"菜单

-- 一级菜单改名
UPDATE system_menu SET menu_name = '数据探查' WHERE menu_id = 2723;

-- 隐藏旧的"质量探查任务"独立菜单
UPDATE system_menu SET status = '1' WHERE menu_id = 2752;

-- 隐藏旧的"数据探查"子目录(2555)及其子菜单
UPDATE system_menu SET status = '1' WHERE menu_id = 2555;
UPDATE system_menu SET status = '1' WHERE menu_id = 2563;
UPDATE system_menu SET status = '1' WHERE menu_id = 2564;
UPDATE system_menu SET status = '1' WHERE menu_id = 2565;

-- 2725 探查任务 改名为"探查任务"（保持不变，只是确认）
UPDATE system_menu SET menu_name = '探查任务' WHERE menu_id = 2725;

-- 新增"探查结果"菜单（挂在2724元数据管理下）
INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (
    2760, '探查结果', 2724, 3, 'probeResult', 'cat/probeResult/index', NULL, 'ProbeResult',
    1, 0, 'C', '0', '0', 'cat:probeResult:list', 'eye-line',
    '超级管理员', NOW(), NULL, NULL, NULL
) ON CONFLICT (menu_id) DO NOTHING;

-- 探查结果子按钮
INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2761, '探查结果查询', 2760, 1, '#', '', NULL, NULL, 1, 0, 'F', '0', '0', 'cat:probeResult:query', '#', '超级管理员', NOW(), NULL, NULL, NULL),
(2762, '探查结果详情', 2760, 2, '#', '', NULL, NULL, 1, 0, 'F', '0', '0', 'cat:probeResult:detail', '#', '超级管理员', NOW(), NULL, NULL, NULL)
ON CONFLICT (menu_id) DO NOTHING;

-- 确保"数据源管理"菜单存在（检查是否已有，没有则新增）
INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 2763, '数据源管理', 2724, 0, 'datasource', 'cat/datasource/index', NULL, 'CatDatasource',
    1, 0, 'C', '0', '0', 'cat:datasource:list', 'database-2-line',
    '超级管理员', NOW(), NULL, NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = 2724 AND path = 'datasource' AND menu_type = 'C' AND status = '0'
) ON CONFLICT (menu_id) DO NOTHING;
