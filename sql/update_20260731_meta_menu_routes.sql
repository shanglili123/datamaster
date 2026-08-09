-- 元数据采集（cat）前端视图归一为 meta 后的菜单 component 迁移
-- 说明：2724 元数据管理 path 已是 meta（/dg/meta）；2727/2728 component 已指向 meta/...；
-- 仅 2725/2726 采集任务/采集实例仍残留 cat/ component，此处一并改为 meta/。
-- 验证：SELECT menu_id, menu_name, path, component FROM system_menu WHERE menu_id IN (2724,2725,2726,2727,2728) ORDER BY menu_id;

UPDATE system_menu SET component = 'meta/task/structured/index', update_time = now() WHERE menu_id = 2725;
UPDATE system_menu SET component = 'meta/instance/structured/index', update_time = now() WHERE menu_id = 2726;
