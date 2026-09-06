-- =============================================
-- 本体模型菜单注册 SQL (PostgreSQL)
-- 注意: system_menu 主键为 menu_id 且非自增, 需显式赋值
-- 幂等: 可重复执行
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- !! 本文件为 UTF-8 编码, 执行前必须保证客户端按 UTF-8 读取:
-- !!   psql    : 先执行 \encoding UTF8, 或设置环境变量 PGCLIENTENCODING=UTF8
-- !!             切勿用 PowerShell Get-Content 管道灌入(会按 GBK 误读导致中文乱码)
-- !!   Navicat: 查询窗口编码选择 65001 (UTF-8)
-- !! 若菜单名已出现乱码, 直接重跑本脚本即可整体重建
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
SET client_encoding = 'UTF8';
BEGIN;

-- 清理旧的 ont 菜单(含子孙节点)
DELETE FROM system_menu WHERE menu_id IN (
    WITH RECURSIVE ont_tree AS (
        SELECT menu_id FROM system_menu WHERE path = 'ont' AND parent_id = 0
        UNION ALL
        SELECT m.menu_id FROM system_menu m JOIN ont_tree t ON m.parent_id = t.menu_id
    )
    SELECT menu_id FROM ont_tree
);
DELETE FROM system_menu WHERE component LIKE 'ont/%';

DO $$
DECLARE
    v_top BIGINT;
BEGIN
    SELECT COALESCE(MAX(menu_id), 0) + 100 INTO v_top FROM system_menu;

    -- 一级目录: 本体模型
    INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    VALUES (v_top, '本体模型', 0, 8, 'ont', NULL, NULL, '', 1, 1, 'M', '0', '0', '', 'mind-map', 'admin', NOW(), '本体模型管理菜单');

    -- 二级页面
    INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    VALUES
        (v_top + 1, '本体管理',   v_top, 1, 'ontology', 'ont/ontology/index', NULL, '', 1, 1, 'C', '0', '0', 'ont:ontology:list', 'organization-chart', 'admin', NOW(), ''),
        (v_top + 2, '函数管理',   v_top, 2, 'function', 'ont/function/index', NULL, '', 1, 1, 'C', '0', '0', 'ont:function:list', 'tool',       'admin', NOW(), '');

    -- 隐藏页面(visible=1): 本体工作台 / 概念管理 / 对象实例
    -- 动态路由会整体替换前端同名静态路由(Ont), 这几个隐藏行保证带参数的详情页不丢
    INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    VALUES
        (v_top + 5, '本体工作台', v_top, 5, 'workspace/:ontologyId', 'ont/workspace/index', NULL, 'OntWorkspace', 1, 1, 'C', '1', '0', 'ont:ontology:query', 'dashboard', 'admin', NOW(), ''),
        (v_top + 6, '概念管理',   v_top, 6, 'concept/:ontologyId',   'ont/concept/index',   NULL, 'ConceptManage', 1, 1, 'C', '1', '0', 'ont:concept:list',   'node-tree', 'admin', NOW(), ''),
        (v_top + 7, '对象实例',   v_top, 7, 'object/:ontologyId',   'ont/object/index',    NULL, 'OntObjectInstance', 1, 1, 'C', '1', '0', 'ont:object-instance:query', 'table', 'admin', NOW(), '对象实例浏览器（对象血缘入口，从本体管理卡片按钮跳转）');

    -- 三级按钮权限
    -- 覆盖: 本体管理 / 函数管理 / 工作台画布(概念·属性·关系·绑表·映射) / 独立概念页
    -- 缺失这些权限码时, 前端 v-hasPermi 会隐藏画布上的 编辑/删除/绑定表/属性映射/关联表字段 按钮,
    -- 后端 @PreAuthorize 也会拒绝 /ont/graph/data (ont:concept:list) 等接口
    INSERT INTO system_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
    SELECT v_top + 10 + ROW_NUMBER() OVER (ORDER BY b.comp, b.ord),
           b.label, p.menu_id, b.ord, '#', '', NULL, '', 1, 1, 'F', '0', '0', b.perm, '#', 'admin', NOW(), ''
    FROM system_menu p
    JOIN (VALUES
        ('本体管理查询', 'ont/ontology/index', 'ont:ontology:query',  1),
        ('本体管理新增', 'ont/ontology/index', 'ont:ontology:add',    2),
        ('本体管理修改', 'ont/ontology/index', 'ont:ontology:edit',   3),
        ('本体管理删除', 'ont/ontology/index', 'ont:ontology:remove', 4),
        ('本体管理导出', 'ont/ontology/index', 'ont:ontology:export', 5),
        ('函数查询',     'ont/function/index', 'ont:function:query',  1),
        ('函数新增',     'ont/function/index', 'ont:function:add',    2),
        ('函数修改',     'ont/function/index', 'ont:function:edit',   3),
        ('函数删除',     'ont/function/index', 'ont:function:remove', 4),
        ('函数执行',     'ont/function/index', 'ont:function:exec',   5),
        ('函数审批',     'ont/function/index', 'ont:function:approve',6),
        -- ===== 本体工作台(可视化图谱 + 概念/属性/关系面板 + 绑定弹窗) =====
        ('图谱-概念列表',   'ont/workspace/index', 'ont:concept:list',        1),
        ('图谱-概念详情',   'ont/workspace/index', 'ont:concept:query',       2),
        ('图谱-概念新增',   'ont/workspace/index', 'ont:concept:add',         3),
        ('图谱-概念修改',   'ont/workspace/index', 'ont:concept:edit',        4),
        ('图谱-概念删除',   'ont/workspace/index', 'ont:concept:remove',      5),
        ('工作台-属性列表', 'ont/workspace/index', 'ont:property:list',       6),
        ('工作台-属性详情', 'ont/workspace/index', 'ont:property:query',      7),
        ('工作台-属性新增', 'ont/workspace/index', 'ont:property:add',        8),
        ('工作台-属性修改', 'ont/workspace/index', 'ont:property:edit',       9),
        ('工作台-属性删除', 'ont/workspace/index', 'ont:property:remove',    10),
        ('工作台-关系列表', 'ont/workspace/index', 'ont:relation:list',      11),
        ('工作台-关系详情', 'ont/workspace/index', 'ont:relation:query',     12),
        ('工作台-关系新增', 'ont/workspace/index', 'ont:relation:add',       13),
        ('工作台-关系修改', 'ont/workspace/index', 'ont:relation:edit',      14),
        ('工作台-关系删除', 'ont/workspace/index', 'ont:relation:remove',    15),
        ('工作台-绑表列表', 'ont/workspace/index', 'ont:concept-table:list', 16),
        ('工作台-绑表详情', 'ont/workspace/index', 'ont:concept-table:query',17),
        ('工作台-概念绑表', 'ont/workspace/index', 'ont:concept-table:add',  18),
        ('工作台-绑表修改', 'ont/workspace/index', 'ont:concept-table:edit', 19),
        ('工作台-概念解绑', 'ont/workspace/index', 'ont:concept-table:remove',20),
        ('工作台-属性映射查询', 'ont/workspace/index', 'ont:property-column:list',  21),
        ('工作台-属性映射保存', 'ont/workspace/index', 'ont:property-column:edit',  22),
        ('工作台-属性映射删除', 'ont/workspace/index', 'ont:property-column:remove',23),
        ('工作台-关联字段映射查询', 'ont/workspace/index', 'ont:relation-column:list',  24),
        ('工作台-关联字段映射保存', 'ont/workspace/index', 'ont:relation-column:edit',  25),
        ('工作台-关联字段映射删除', 'ont/workspace/index', 'ont:relation-column:remove',26),
        ('工作台-关联表绑定查询',   'ont/workspace/index', 'ont:relation-table:list',   27),
        ('工作台-关联表绑定详情',   'ont/workspace/index', 'ont:relation-table:query',  28),
        ('工作台-关联表绑定新增',   'ont/workspace/index', 'ont:relation-table:add',    29),
        ('工作台-关联表绑定修改',   'ont/workspace/index', 'ont:relation-table:edit',   30),
        ('工作台-关联表绑定删除',   'ont/workspace/index', 'ont:relation-table:remove', 32),
        -- ===== 本体工作台-动作管理(动作 CRUD + 执行审批流) =====
        ('工作台-动作列表', 'ont/workspace/index', 'ont:action:list',   33),
        ('工作台-动作详情', 'ont/workspace/index', 'ont:action:query',  34),
        ('工作台-动作新增', 'ont/workspace/index', 'ont:action:add',    35),
        ('工作台-动作修改', 'ont/workspace/index', 'ont:action:edit',   36),
        ('工作台-动作删除', 'ont/workspace/index', 'ont:action:remove', 37),
        -- ===== 独立概念管理页 =====
        ('概念查询', 'ont/concept/index', 'ont:concept:list',   1),
        ('概念详情', 'ont/concept/index', 'ont:concept:query',  2),
        ('概念新增', 'ont/concept/index', 'ont:concept:add',    3),
        ('概念修改', 'ont/concept/index', 'ont:concept:edit',   4),
        ('概念删除', 'ont/concept/index', 'ont:concept:remove', 5),
        -- ===== 对象实例浏览器(对象血缘四维度入口) =====
        -- 对象实例/对象血缘接口统一使用 ont:object-instance:query（见 ObjectInstanceController）
        ('对象实例查询', 'ont/object/index', 'ont:object-instance:query', 1)
    ) AS b(label, comp, perm, ord) ON p.component = b.comp AND p.parent_id = v_top;
END $$;

COMMIT;
