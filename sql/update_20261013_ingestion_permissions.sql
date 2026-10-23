-- Ingestion 管理控制台及死信重放权限。
-- 依赖 system_menu、system_role_menu、system_role 已由平台基础 SQL 创建。

INSERT INTO public.system_menu
    (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 4150, '数据接入', 2, 20, 'ingestion', 'ingestion/index',
       1, 0, 'C', '0', '0', 'ingestion:query', 'exchange-line', 'system', CURRENT_TIMESTAMP,
       'Kafka 数据接入运行状态、指标和死信管理'
WHERE NOT EXISTS (SELECT 1 FROM public.system_menu WHERE menu_id = 4150);

INSERT INTO public.system_menu
    (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT v.menu_id, v.menu_name, 4150, v.order_num, '#', NULL, 1, 0, 'F', '0', '0', v.perms, '#', 'system', CURRENT_TIMESTAMP, NULL
FROM (VALUES
    (4151, '接入查询', 1, 'ingestion:query'),
    (4152, '死信重放', 2, 'ingestion:replay'),
    (4153, '消费启停', 3, 'ingestion:control')
) AS v(menu_id, menu_name, order_num, perms)
WHERE NOT EXISTS (SELECT 1 FROM public.system_menu m WHERE m.menu_id = v.menu_id);

INSERT INTO public.system_role_menu (role_id, menu_id, space_id)
SELECT r.role_id, m.menu_id, 0
FROM public.system_role r
JOIN public.system_menu m ON m.menu_id BETWEEN 4150 AND 4153
WHERE r.role_id IN (1, 2)
  AND NOT EXISTS (
      SELECT 1 FROM public.system_role_menu rm
      WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id AND COALESCE(rm.space_id, 0) = 0
  );
