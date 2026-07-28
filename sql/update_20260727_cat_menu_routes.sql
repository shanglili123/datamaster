-- Unify metadata catalog menu routes under /dg/cat.
-- Applied to local dev database on 2026-07-27.

UPDATE public.system_menu
SET path = 'cat',
    visible = '0',
    status = '0'
WHERE menu_id = 2724;

UPDATE public.system_menu
SET parent_id = 2724,
    component = 'ast/datasource/index'
WHERE menu_id = 2336;

UPDATE public.system_menu
SET parent_id = 2724
WHERE menu_id = 2725;

UPDATE public.system_menu
SET component = 'cat/unreleased/structured/table/index'
WHERE menu_id = 2727;

UPDATE public.system_menu
SET component = CASE
        WHEN component LIKE 'da/%' THEN regexp_replace(component, '^da/', 'ast/')
        WHEN component LIKE 'dpp/%' THEN regexp_replace(component, '^dpp/', 'col/')
        WHEN component LIKE 'ds/%' THEN regexp_replace(component, '^ds/', 'svc/')
        WHEN component LIKE 'dm/%' THEN regexp_replace(component, '^dm/', 'mdl/')
        WHEN component LIKE 'meta/%' THEN regexp_replace(component, '^meta/', 'cat/')
        ELSE component
    END
WHERE component LIKE 'da/%'
   OR component LIKE 'dpp/%'
   OR component LIKE 'ds/%'
   OR component LIKE 'dm/%'
   OR component LIKE 'meta/%';

UPDATE public.system_menu
SET perms = replace(
        replace(
            replace(
                replace(
                    replace(perms, 'meta:', 'cat:'),
                    'dpp:', 'col:'
                ),
                'da:', 'ast:'
            ),
            'ds:', 'svc:'
        ),
        'dm:', 'mdl:'
    )
WHERE perms LIKE '%da:%'
   OR perms LIKE '%dpp:%'
   OR perms LIKE '%ds:%'
   OR perms LIKE '%dm:%'
   OR perms LIKE '%meta:%';

UPDATE public.system_menu
SET perms = replace(perms, 'mc:metadata:table:', 'cat:table:')
WHERE perms LIKE '%mc:metadata:table:%';

UPDATE public.system_menu
SET parent_id = 1,
    order_num = 20
WHERE menu_id = 2243;

UPDATE public.system_menu
SET perms = replace(perms, 'att:project:', 'tax:project:')
WHERE menu_id IN (2243, 2244, 2245, 2246, 2247, 2248, 2249)
  AND perms LIKE 'att:project:%'
  AND perms NOT LIKE 'att:project:role:%';

UPDATE public.system_menu
SET perms = replace(
        replace(
            replace(
                replace(perms, 'att:projectUserRel:', 'col:projectUserRel:'),
                'att:project:role:',
                'col:project:role:'
            ),
            'att:taskCat:',
            'col:taskCat:'
        ),
        'att:dataDevCat:',
        'col:dataDevCat:'
    )
WHERE perms LIKE '%att:projectUserRel:%'
   OR perms LIKE '%att:project:role:%'
   OR perms LIKE '%att:taskCat:%'
   OR perms LIKE '%att:dataDevCat:%';

UPDATE public.system_menu
SET perms = replace(
        replace(
            replace(perms, 'att:clientApiRel:', 'svc:clientApiRel:'),
            'att:client:',
            'svc:client:'
        ),
        'att:apiCat:',
        'svc:apiCat:'
    )
WHERE perms LIKE '%att:clientApiRel:%'
   OR perms LIKE '%att:client:%'
   OR perms LIKE '%att:apiCat:%';

UPDATE public.system_menu
SET perms = replace(perms, 'att:', 'tax:')
WHERE perms LIKE '%att:%';

UPDATE public.system_menu
SET menu_name = replace(menu_name, '项目', '空间')
WHERE menu_name LIKE '%项目%';
