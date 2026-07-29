UPDATE system_menu
SET menu_name = '标准建模',
    update_time = NOW()
WHERE menu_id = 2694
   OR (parent_id = 0 AND menu_name = '数据建模' AND path IN ('dm', 'mdl'));
