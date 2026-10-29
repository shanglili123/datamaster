-- 菜单定义以 system_menu 为唯一来源；配合前端移除菜单重命名、搬迁、补入口逻辑。
-- PostgreSQL，可重复执行；执行前备份菜单数据，执行后重新登录以刷新菜单/权限缓存。
-- 不删除菜单、不修改已有 parent/path/角色/空间授权，不启用已禁用菜单。
-- 数据统计若新增，只创建菜单，不自动授权；管理员需核对 cat:db:list、cat:table:list 后分配。
BEGIN;
LOCK TABLE public.system_menu IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    metadata_id bigint;
    statistics_id bigint;
    candidate_count integer;
BEGIN
    SELECT count(*), min(menu_id) INTO candidate_count, metadata_id
    FROM public.system_menu
    WHERE parent_id = 0 AND path = 'meta' AND menu_type = 'M';
    IF candidate_count <> 1 THEN
        RAISE EXCEPTION '需要且只能有一个 path=meta 的元数据根目录，实际 % 个；请先在菜单管理核对', candidate_count;
    END IF;

    UPDATE public.system_menu
    SET menu_name = '元数据管理', update_by = 'system', update_time = CURRENT_TIMESTAMP
    WHERE menu_id = metadata_id
      AND menu_name IN ('数据探查', '数据治理')
      AND menu_name IS DISTINCT FROM '元数据管理';

    -- 复用旧探查结果的菜单 ID、path、perms 和既有授权，不另建同名入口。
    -- 旧 cat:probeResult:list 不自动升级成 cat:table:list；权限不足者需由管理员另行审核。
    UPDATE public.system_menu
    SET component = 'meta/catalog/table/index', update_by = 'system', update_time = CURRENT_TIMESTAMP
    WHERE component = 'meta/unreleased/structured/table/index'
      AND menu_type = 'C';

    UPDATE public.system_menu
    SET menu_name = '探查元数据', update_by = 'system', update_time = CURRENT_TIMESTAMP
    WHERE component = 'meta/catalog/table/index' AND menu_name = '元数据结果';

    UPDATE public.system_menu
    SET menu_name = v.new_name, update_by = 'system', update_time = CURRENT_TIMESTAMP
    FROM (VALUES
        ('数据连接', '数据源管理', 'ast/datasource/index'),
        ('数据开发类目', '数据开发目录', 'col/setting/dataDevCat/index'),
        ('数据集成类目', '数据集成目录', 'col/setting/taskCat/index')
    ) AS v(old_name, new_name, expected_component)
    WHERE menu_name = v.old_name AND component = v.expected_component;

    UPDATE public.system_menu
    SET menu_name = '目录管理', update_by = 'system', update_time = CURRENT_TIMESTAMP
    WHERE menu_type = 'M' AND path = 'cat' AND menu_name = '类目管理'
      AND parent_id IN (SELECT menu_id FROM public.system_menu WHERE parent_id = 0 AND path = 'spaceBase');

    UPDATE public.system_menu
    SET menu_name = '研发目录管理', update_by = 'system', update_time = CURRENT_TIMESTAMP
    WHERE menu_type = 'M' AND route_name = 'ColDevelopmentCategory' AND menu_name = '研发类目管理';

    SELECT count(*), min(menu_id) INTO candidate_count, statistics_id
    FROM public.system_menu
    WHERE component = 'meta/catalog/statistics/index' OR route_name = 'MetadataStatistics';
    IF candidate_count > 1 THEN
        RAISE EXCEPTION '数据统计存在重复菜单，请在菜单管理合并后重试';
    ELSIF candidate_count = 1 THEN
        IF NOT EXISTS (SELECT 1 FROM public.system_menu WHERE menu_id = statistics_id
                       AND component = 'meta/catalog/statistics/index' AND menu_type = 'C') THEN
            RAISE EXCEPTION 'MetadataStatistics 路由名已被其他组件占用';
        END IF;
    ELSE
        IF EXISTS (SELECT 1 FROM public.system_menu
                   WHERE parent_id = metadata_id AND path = 'statistics') THEN
            RAISE EXCEPTION '元数据 statistics 路径已被其他菜单占用';
        END IF;
        SELECT COALESCE(max(menu_id), 0) + 1 INTO statistics_id FROM public.system_menu;
        INSERT INTO public.system_menu
            (menu_id, menu_name, parent_id, order_num, path, component, route_name,
             is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
        VALUES
            (statistics_id, '数据统计', metadata_id, 99, 'statistics', 'meta/catalog/statistics/index',
             'MetadataStatistics', 1, 0, 'C', '0', '0', 'cat:table:list', 'bar-chart-box-line',
             'system', CURRENT_TIMESTAMP, '元数据统计入口；角色授权需管理员审核，不随迁移自动分配');
    END IF;
END $$;

COMMIT;

-- 只读核对：菜单组件和层级由后端管理，下面不自动修改任何角色权限。
SELECT menu_id, menu_name, parent_id, order_num, path, component, route_name, visible, status, perms
FROM public.system_menu
WHERE component IN ('meta/catalog/table/index', 'meta/catalog/statistics/index')
   OR (parent_id = 0 AND path IN ('meta', 'spaceBase', 'col'))
ORDER BY parent_id, order_num, menu_id;

-- 使用新版元数据浏览页还需要 cat:table:list；旧探查结果权限不在本脚本中自动扩权。
SELECT DISTINCT r.role_id, r.role_name, r.space_id, m.menu_id, m.perms
FROM public.system_role r
JOIN public.system_role_menu rm ON rm.role_id = r.role_id
JOIN public.system_menu m ON m.menu_id = rm.menu_id
WHERE m.component = 'meta/catalog/table/index'
  AND position('cat:table:list' IN COALESCE(m.perms, '')) = 0
ORDER BY r.role_id, m.menu_id;
