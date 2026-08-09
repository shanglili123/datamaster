-- 2026-08-03: Fix orphan menus after migration scripts
-- Menu 2730 (标准建模组) and 2306 (数据元组) were missing, causing children to be orphaned

-- Step 1: Re-parent 2307 (创建模型) and 2731 (发布模型) under 2694 (标准建模)
UPDATE system_menu SET parent_id = 2694, order_num = 5 WHERE menu_id = 2307;
UPDATE system_menu SET parent_id = 2694, order_num = 6 WHERE menu_id = 2731;

-- Step 2: Re-parent 2376/2383/2390 (hidden menus) under 2315 (标准数据元)
UPDATE system_menu SET parent_id = 2315, order_num = 7 WHERE menu_id = 2376;
UPDATE system_menu SET parent_id = 2315, order_num = 8 WHERE menu_id = 2383;
UPDATE system_menu SET parent_id = 2315, order_num = 9 WHERE menu_id = 2390;

-- Step 3: Replace stale role_menu references (2730→2694, 2306→2315)
UPDATE system_role_menu SET menu_id = 2694 WHERE menu_id = 2730;
UPDATE system_role_menu SET menu_id = 2315 WHERE menu_id = 2306;

-- Step 4: Fix top-level order duplicates (2314 and 2552 both at order=3)
UPDATE system_menu SET order_num = 4 WHERE menu_id = 2552;
UPDATE system_menu SET order_num = 5 WHERE menu_id = 2397;
UPDATE system_menu SET order_num = 6 WHERE menu_id = 2427;
UPDATE system_menu SET order_num = 7 WHERE menu_id = 2733;

-- Step 5: Fix order duplicates under system management (parent=1)
UPDATE system_menu SET order_num = 8 WHERE menu_id = 2026;
UPDATE system_menu SET order_num = 9 WHERE menu_id = 2062;

-- Step 6: Verify no orphans and no top-level order duplicates remain
-- SELECT m.menu_id, m.menu_name, m.parent_id FROM system_menu m
-- LEFT JOIN system_menu p ON m.parent_id = p.menu_id
-- WHERE m.parent_id != 0 AND p.menu_id IS NULL;
-- SELECT order_num, count(*), array_agg(menu_id || ':' || menu_name) FROM system_menu
-- WHERE parent_id = 0 GROUP BY order_num HAVING count(*) > 1;
