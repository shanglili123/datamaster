-- 探查任务统一改造：采集任务升级为探查任务（可选挂质量规则）
-- 1. 新建探查任务-质量任务关联表 cat_task_quality
-- 2. 菜单 2725 采集任务 -> 探查任务；隐藏无视图菜单 2726 采集实例 / 2728 定版元数据

CREATE TABLE IF NOT EXISTS cat_task_quality (
    id bigserial PRIMARY KEY,
    cat_task_id bigint NOT NULL,
    quality_task_id bigint NOT NULL,
    create_by varchar(64),
    creator_id bigint,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    updater_id bigint,
    update_time timestamp,
    remark varchar(500),
    del_flag varchar(1) DEFAULT '0'
);

COMMENT ON TABLE cat_task_quality IS '探查任务-质量探查任务关联表';
COMMENT ON COLUMN cat_task_quality.cat_task_id IS '探查任务ID（CAT_TASK.id）';
COMMENT ON COLUMN cat_task_quality.quality_task_id IS '质量探查任务ID（COL_QUALITY_TASK.id）';

CREATE INDEX IF NOT EXISTS idx_cat_task_quality_cat ON cat_task_quality(cat_task_id);
CREATE INDEX IF NOT EXISTS idx_cat_task_quality_quality ON cat_task_quality(quality_task_id);

-- 菜单归一
UPDATE system_menu SET menu_name = '探查任务', update_time = now() WHERE menu_id = 2725;
UPDATE system_menu SET status = '1', update_time = now() WHERE menu_id IN (2726, 2728);

-- 验证：
-- SELECT menu_id, menu_name, path, component, status FROM system_menu WHERE menu_id IN (2724,2725,2726,2727,2728) ORDER BY menu_id;
