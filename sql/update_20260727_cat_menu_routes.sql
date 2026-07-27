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
