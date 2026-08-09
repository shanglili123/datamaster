-- 2026-08-04 数据探查菜单二级目录调整：
--   1) 探查结果(2727) 更名为 探查元数据
--   2) 新增 探查任务日志(2764) -> 数据探查 > 探查任务日志(采集任务实例日志)
UPDATE system_menu SET menu_name = '探查元数据', update_time = now() WHERE menu_id = 2727;

-- 新增菜单 C：探查任务日志
INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, route_name, menu_type, visible, status, perms, icon, create_time, update_time)
VALUES (2764, '探查任务日志', 2723, 4, 'taskInstance', 'meta/task/structured/instance/index', 1, 0, '', 'C', '0', '0', 'cat:taskInstance:list', 'file-list-3-line', now(), now());

-- 新增按钮级权限 F(menu_id 2765-2767)
INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, route_name, menu_type, visible, status, perms, icon, create_time, update_time)
VALUES
    (2765, '查询', 2764, 1, '', '', 1, 0, '', 'F', '0', '0', 'cat:taskInstance:query', '#', now(), now()),
    (2766, '详情', 2764, 2, '', '', 1, 0, '', 'F', '0', '0', 'cat:taskInstance:info', '#', now(), now()),
    (2767, '日志', 2764, 3, '', '', 1, 0, '', 'F', '0', '0', 'cat:taskInstance:log', '#', now(), now());
