-- 2026-07-31 质量探查任务菜单：并入元数据(2724)下，替代失效的旧菜单(2565)
-- 新增菜单 C：质量探查任务(menu_id 2752) -> 元数据管理 > 质量探查任务
INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, route_name, menu_type, visible, status, perms, icon, create_time, update_time)
VALUES (2752, '质量探查任务', 2724, 4, 'qualityTask', 'ast/quality/qualityTask/index', 1, 0, '', 'C', '0', '0', 'ast:qualityTask:query', '#', now(), now());

-- 新增按钮级权限 F(menu_id 2753-2758)
INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, route_name, menu_type, visible, status, perms, icon, create_time, update_time)
VALUES
    (2753, '查询', 2752, 1, '', '', 1, 0, '', 'F', '0', '0', 'ast:qualityTask:query', '#', now(), now()),
    (2754, '新增', 2752, 2, '', '', 1, 0, '', 'F', '0', '0', 'ast:qualityTask:add', '#', now(), now()),
    (2755, '修改', 2752, 3, '', '', 1, 0, '', 'F', '0', '0', 'ast:qualityTask:edit', '#', now(), now()),
    (2756, '删除', 2752, 4, '', '', 1, 0, '', 'F', '0', '0', 'ast:qualityTask:remove', '#', now(), now()),
    (2757, '详情', 2752, 5, '', '', 1, 0, '', 'F', '0', '0', 'ast:qualityTask:info', '#', now(), now()),
    (2758, '执行一次', 2752, 6, '', '', 1, 0, '', 'F', '0', '0', 'ast:qualityTask:once', '#', now(), now());

-- 隐藏失效的旧菜单：2565 数据质量任务(component 指向不存在的 da/quality/qualityTask/index)
UPDATE system_menu SET status = '1' WHERE menu_id = 2565;
