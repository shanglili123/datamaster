-- ============================================================
-- 菜单与字典改名:空间基础管理->空间管理 / 资产地图->资产数据
-- 日期:2026-08-16
-- 数据库:PostgreSQL(datamaster @ 192.168.93.174)
--
-- 背景:
--   1. 顶级目录"空间基础管理"(menu 2552)改名为"空间管理";
--      原系统管理下"空间管理"(menu 2243,空间列表页面)改名为"空间列表",避免重名。
--   2. 数据资产下"资产地图"(menu 2329)改名为"资产数据";
--      其查询按钮(menu 2538)同步改为"资产数据查询"。
--   3. 相关字典类型名称同步改名(dict_type 编码不变):
--      79  资产地图数据来源          -> 资产数据来源
--      86  资产地图-矢量文件类型     -> 资产数据-矢量文件类型
--      87  资产地图-视频平台         -> 资产数据-视频平台
--
-- 代码侧需同步:
--   datamaster-ui/src/store/system/permission.js
--     isSpaceBaseManagement 匹配 '空间管理';isSpaceManagement 匹配 '空间列表'
--   datamaster-common/.../security/AccessPolicy.java
--     isSpaceBaseMenu 按 '空间管理' 判断
-- ============================================================

-- 1. 顶级目录改名
UPDATE system_menu SET menu_name = '空间管理' WHERE menu_id = 2552;

-- 2. 系统管理下原"空间管理"(空间列表页)改名,避免与顶级重名
UPDATE system_menu SET menu_name = '空间列表' WHERE menu_id = 2243;

-- 3. 资产地图 -> 资产数据
UPDATE system_menu SET menu_name = '资产数据' WHERE menu_id = 2329;
UPDATE system_menu SET menu_name = '资产数据查询' WHERE menu_id = 2538;

-- 4. 字典类型名称同步(dict_type 编码不变)
UPDATE system_dict_type SET dict_name = '资产数据来源' WHERE dict_id = 79;
UPDATE system_dict_type SET dict_name = '资产数据-矢量文件类型' WHERE dict_id = 86;
UPDATE system_dict_type SET dict_name = '资产数据-视频平台' WHERE dict_id = 87;
