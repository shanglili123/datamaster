-- ============================================================================
-- 2026-08-05 元数据/数据探查菜单前缀统一清理
-- 目标：数据库菜单路径与前端路由前缀(/meta/*)对齐，清理旧前缀 dg / cat / att
-- 前置：update_20260731_meta_menu_routes.sql、update_20260803_explore_menu.sql、
--       update_20260804_explore_subdir.sql 均已执行
-- 背景：
--   1) 菜单顶级 2723 仍用旧前缀 path='dg'，二级 2724 为 'cat'/'meta' 不确定，
--      与前端路由 /meta/*（router/meta/dynamic）不一致，侧边栏高亮按路由第一段匹配，旧前缀导致不高亮。
--   2) 2725/2727 的 component 仍指向不存在的 views/cat/ 目录（点击 404），
--      实际视图在 views/meta/ 下（对应组件已由 7/31 脚本改为 meta/ 前缀，此处兜底修正）。
--   3) 2760 探查结果指向未开发的 views/meta/probeResult，先停用避免 404。
-- PostgreSQL migration
-- 验证:
--   SELECT menu_id, menu_name, path, component, status, visible
--   FROM system_menu WHERE menu_id IN (2723, 2724, 2725, 2727, 2760, 2763) ORDER BY menu_id;
-- ============================================================================

-- 1) 顶级菜单 2723(数据探查) path: dg -> meta（与前端 /meta/* 路由对齐）
UPDATE system_menu SET path = 'meta', update_time = now() WHERE menu_id = 2723 AND path = 'dg';

-- 2) 二级菜单 2724(元数据管理) path: cat 或 meta -> catalog（避免与顶级 meta 重复）
UPDATE system_menu SET path = 'catalog', update_time = now() WHERE menu_id = 2724 AND path IN ('cat', 'meta');

-- 3) 2725 探查任务 component: cat/ -> meta/（视图 views/meta/task/structured/index.vue）
UPDATE system_menu SET component = 'meta/task/structured/index', update_time = now()
WHERE menu_id = 2725 AND component LIKE 'cat/%';

-- 4) 2727 探查元数据 component: cat/ -> meta/（视图 views/meta/unreleased/structured/table/index.vue）
UPDATE system_menu SET component = 'meta/unreleased/structured/table/index', update_time = now()
WHERE menu_id = 2727 AND component LIKE 'cat/%';

-- 5) 2760 探查结果: component 指向未开发的 views/meta/probeResult 视图，先停用（前端开发完成后恢复 status='0'）
UPDATE system_menu SET status = '1', update_time = now() WHERE menu_id = 2760;

-- 6) 2763 数据源管理(20260803 条件插入，可能存在): component cat/ -> ast/（与 2336 数据源管理一致）
UPDATE system_menu SET component = 'ast/datasource/index', update_time = now()
WHERE menu_id = 2763 AND component LIKE 'cat/%';

-- 注：2375 基础管理(path='att') 为停用+隐藏空壳顶级菜单（无子菜单），不影响渲染，本次不处理。
-- 2368/2383/2390 等 dp/ 旧前缀菜单已停用或隐藏，本次不处理。
