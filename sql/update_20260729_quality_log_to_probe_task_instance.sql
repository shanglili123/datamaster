UPDATE system_menu
SET menu_name = '探查任务实例',
    path = 'probeTaskInstance',
    component = 'ast/quality/probeTaskInstance/index',
    perms = 'ast:probeTaskInstance:index',
    update_time = NOW()
WHERE menu_id = 2563
   OR path = 'qualityTaskLog'
   OR menu_name = '质量任务日志';

UPDATE system_menu
SET menu_name = '探查任务实例详情',
    perms = 'ast:probeTaskInstance:detail',
    update_time = NOW()
WHERE menu_id = 2564
   OR (parent_id = 2563 AND menu_name = '质量任务日志详情')
   OR perms = 'dp:qualityLog:edit';
