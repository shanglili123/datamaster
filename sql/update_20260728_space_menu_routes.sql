-- 2026-07-28: align workspace/menu taxonomy from Project to Space.
-- Run against the server PostgreSQL database after deploying the renamed frontend/backend code.

UPDATE system_menu
SET
  menu_name = replace(replace(menu_name, '项目与用户关联关系', '空间成员'), '项目', '空间'),
  path = CASE path
    WHEN 'projectBase' THEN 'spaceBase'
    WHEN 'project' THEN 'space'
    WHEN 'projectUserRel' THEN 'spaceUserRel'
    ELSE replace(replace(path, 'projectUserRel', 'spaceUserRel'), 'projectBase', 'spaceBase')
  END,
  component = CASE component
    WHEN 'att/project/index' THEN 'tax/space/index'
    WHEN 'tax/project/index' THEN 'tax/space/index'
    WHEN 'dpp/setting/projectUserRel/index' THEN 'col/setting/spaceUserRel/index'
    WHEN 'col/setting/projectUserRel/index' THEN 'col/setting/spaceUserRel/index'
    ELSE replace(replace(component, 'projectUserRel', 'spaceUserRel'), 'tax/project', 'tax/space')
  END,
  route_name = CASE route_name
    WHEN 'ProjectBaseManagement' THEN 'SpaceBaseManagement'
    ELSE replace(route_name, 'Project', 'Space')
  END,
  perms = CASE
    WHEN perms LIKE 'att:projectUserRel:%' THEN replace(perms, 'att:projectUserRel:', 'col:spaceUserRel:')
    WHEN perms LIKE 'att:spaceUserRel:%' THEN replace(perms, 'att:spaceUserRel:', 'col:spaceUserRel:')
    WHEN perms LIKE 'att:project:role:%' THEN replace(perms, 'att:project:role:', 'col:space:role:')
    WHEN perms LIKE 'col:project:role:%' THEN replace(perms, 'col:project:role:', 'col:space:role:')
    WHEN perms LIKE 'att:project:%' THEN replace(perms, 'att:project:', 'tax:space:')
    WHEN perms LIKE 'tax:project:%' THEN replace(perms, 'tax:project:', 'tax:space:')
    ELSE perms
  END,
  remark = replace(replace(remark, '项目与用户关联关系', '空间成员'), '项目', '空间')
WHERE
  path IN ('projectBase', 'project', 'projectUserRel')
  OR component IN ('att/project/index', 'tax/project/index', 'dpp/setting/projectUserRel/index', 'col/setting/projectUserRel/index')
  OR path LIKE '%projectUserRel%'
  OR path LIKE '%projectBase%'
  OR component LIKE '%projectUserRel%'
  OR component LIKE '%tax/project%'
  OR route_name LIKE '%Project%'
  OR perms LIKE 'att:project%'
  OR perms LIKE 'tax:project%'
  OR perms LIKE 'att:spaceUserRel%'
  OR perms LIKE 'col:project:role%';

