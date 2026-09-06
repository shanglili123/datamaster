-- =============================================
-- Webhook / 决策 菜单按钮权限注册 SQL (PostgreSQL)
-- 前置: 已执行 sql/ontology_menu.sql, 存在 'ont' 一级目录
-- 幂等: 按权限码(perms)去重, 可重复执行
-- ---------------------------------------------
-- 本文件为 UTF-8 编码, 执行前必须保证客户端按 UTF-8 读取:
--   psql    : 先执行 \encoding UTF8 或设置 PGCLIENTENCODING=UTF8
--   Navicat: 查询窗口编码选择 65001 (UTF-8)
-- =============================================
SET client_encoding = 'UTF8';
BEGIN;

DO $$
DECLARE
    v_ont   BIGINT;   -- 'ont' 一级目录 menu_id
    v_next  BIGINT;   -- 新 menu_id 起点
    v_pid   BIGINT;
BEGIN
    -- 1. 定位 'ont' 一级目录
    SELECT menu_id INTO v_ont FROM system_menu WHERE path = 'ont' AND parent_id = 0 LIMIT 1;
    IF v_ont IS NULL THEN
        RAISE EXCEPTION '未找到 ont 一级目录, 请先执行 sql/ontology_menu.sql';
    END IF;

    -- 2. 新 menu_id 起点(避开已有)
    SELECT COALESCE(MAX(menu_id), 0) + 1000 INTO v_next FROM system_menu;

    -- 3. 决策管理页面(C)
    IF NOT EXISTS (SELECT 1 FROM system_menu WHERE perms = 'ont:decision:list') THEN
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
        VALUES (v_next, '决策管理', v_ont, 30, 'decision', 'ont/decision/index', NULL, '', 1, 1, 'C', '0', '0', 'ont:decision:list', 'decision-tree', 'admin', NOW(), '决策管理(绑定动作 + 数据到达触发)');
        SELECT menu_id INTO v_pid FROM system_menu WHERE perms = 'ont:decision:list' LIMIT 1;
        v_next := v_next + 1;

        -- 5. 决策按钮权限(F)
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (v_next, '决策查询', v_pid, 1, '#', '', NULL, '', 1, 1, 'F', '0', '0', 'ont:decision:query', '#', 'admin', NOW(), ''); v_next := v_next + 1;
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (v_next, '决策新增', v_pid, 2, '#', '', NULL, '', 1, 1, 'F', '0', '0', 'ont:decision:add', '#', 'admin', NOW(), ''); v_next := v_next + 1;
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (v_next, '决策修改', v_pid, 3, '#', '', NULL, '', 1, 1, 'F', '0', '0', 'ont:decision:edit', '#', 'admin', NOW(), ''); v_next := v_next + 1;
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (v_next, '决策删除', v_pid, 4, '#', '', NULL, '', 1, 1, 'F', '0', '0', 'ont:decision:remove', '#', 'admin', NOW(), ''); v_next := v_next + 1;
    END IF;

    -- 4. Webhook 管理页面(C)
    IF NOT EXISTS (SELECT 1 FROM system_menu WHERE perms = 'ont:webhook:list') THEN
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
        VALUES (v_next, 'Webhook回调', v_ont, 31, 'webhook', 'ont/webhook/index', NULL, '', 1, 1, 'C', '0', '0', 'ont:webhook:list', 'link', 'admin', NOW(), 'Webhook回调管理(基于动作/决策执行记录)');
        SELECT menu_id INTO v_pid FROM system_menu WHERE perms = 'ont:webhook:list' LIMIT 1;
        v_next := v_next + 1;

        -- 6. Webhook 按钮权限(F)
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (v_next, 'Webhook查询', v_pid, 1, '#', '', NULL, '', 1, 1, 'F', '0', '0', 'ont:webhook:query', '#', 'admin', NOW(), ''); v_next := v_next + 1;
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (v_next, 'Webhook新增', v_pid, 2, '#', '', NULL, '', 1, 1, 'F', '0', '0', 'ont:webhook:add', '#', 'admin', NOW(), ''); v_next := v_next + 1;
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (v_next, 'Webhook修改', v_pid, 3, '#', '', NULL, '', 1, 1, 'F', '0', '0', 'ont:webhook:edit', '#', 'admin', NOW(), ''); v_next := v_next + 1;
        INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES (v_next, 'Webhook删除', v_pid, 4, '#', '', NULL, '', 1, 1, 'F', '0', '0', 'ont:webhook:remove', '#', 'admin', NOW(), ''); v_next := v_next + 1;
    END IF;
END $$;

COMMIT;