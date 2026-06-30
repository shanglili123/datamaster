INSERT INTO SYSTEM_MENU (
    MENU_ID, MENU_NAME, PARENT_ID, ORDER_NUM, PATH, COMPONENT, QUERY,
    IS_FRAME, IS_CACHE, ROUTE_NAME, MENU_TYPE, VISIBLE, STATUS, PERMS, ICON,
    CREATE_BY, CREATE_TIME, REMARK
)
SELECT
    2760, 'AI运维', 2, 6, 'ai-ops', 'sys/monitor/aiOps/index', NULL,
    1, 0, NULL, 'C', '0', '0', 'monitor:aiops:list', 'znfx',
    'admin', NOW(), 'AI运维诊断菜单'
WHERE NOT EXISTS (
    SELECT 1 FROM SYSTEM_MENU WHERE MENU_ID = 2760 OR COMPONENT = 'sys/monitor/aiOps/index'
);

UPDATE SYSTEM_MENU
SET MENU_NAME = 'AI运维',
    PARENT_ID = 2,
    ORDER_NUM = 6,
    PATH = 'ai-ops',
    COMPONENT = 'sys/monitor/aiOps/index',
    QUERY = NULL,
    MENU_TYPE = 'C',
    VISIBLE = '0',
    STATUS = '0',
    PERMS = 'monitor:aiops:list',
    ICON = 'znfx'
WHERE MENU_ID = 2760 OR COMPONENT = 'sys/monitor/aiOps/index';

INSERT INTO SYSTEM_ROLE_MENU (ROLE_ID, MENU_ID, PROJECT_ID)
SELECT role_menu_source.ROLE_ID, menu.MENU_ID, COALESCE(role_menu_source.PROJECT_ID, 0)
FROM SYSTEM_ROLE_MENU role_menu_source
JOIN SYSTEM_MENU menu ON menu.COMPONENT = 'sys/monitor/aiOps/index'
WHERE role_menu_source.MENU_ID IN (2, 112)
  AND NOT EXISTS (
    SELECT 1 FROM SYSTEM_ROLE_MENU target
    WHERE target.ROLE_ID = role_menu_source.ROLE_ID
      AND target.MENU_ID = menu.MENU_ID
      AND COALESCE(target.PROJECT_ID, -1) = COALESCE(role_menu_source.PROJECT_ID, 0)
);
