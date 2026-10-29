-- PostgreSQL, one-time manual migration. Requires first-batch ontology metric migration.
-- No source data queries, credentials, result storage, role grants, or unique-name constraint.
BEGIN;
CREATE TABLE public.ont_metric_analysis (
    id bigint PRIMARY KEY,
    ontology_id bigint NOT NULL REFERENCES public.ont_ontology(id) ON DELETE RESTRICT,
    owner_id bigint NOT NULL,
    name varchar(100) NOT NULL CHECK (length(btrim(name)) > 0),
    description varchar(1000),
    metric_id bigint NOT NULL,
    version integer NOT NULL CHECK (version > 0),
    query_json text NOT NULL,
    revision integer NOT NULL DEFAULT 0 CHECK (revision >= 0),
    creator_id bigint,
    create_by varchar(50),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updater_id bigint,
    update_by varchar(50),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ont_metric_analysis_version FOREIGN KEY (metric_id, version)
        REFERENCES public.ont_metric_version(metric_id, version) ON DELETE RESTRICT
);
CREATE INDEX idx_ont_metric_analysis_owner_space ON public.ont_metric_analysis(owner_id, ontology_id, id DESC);
CREATE INDEX idx_ont_metric_analysis_version ON public.ont_metric_analysis(metric_id, version);
COMMENT ON TABLE public.ont_metric_analysis IS '本人保存的受控指标查询定义；非逻辑删除；不存结果、SQL、连接凭据；显式版本不自动升级';
COMMENT ON COLUMN public.ont_metric_analysis.owner_id IS '由后端登录用户赋值；管理员也不跨owner读取';
COMMENT ON COLUMN public.ont_metric_analysis.revision IS '更新时行锁保护并要求客户端匹配旧revision';

LOCK TABLE public.system_menu IN SHARE ROW EXCLUSIVE MODE;
DO $$
DECLARE
    ontology_menu_id bigint;
    candidate_count integer;
    next_id bigint;
    existing_parent bigint;
    existing_type varchar;
    permission_record record;
BEGIN
    SELECT count(*), min(menu_id) INTO candidate_count, ontology_menu_id
    FROM public.system_menu WHERE perms = 'ont:ontology:list' AND menu_type = 'C';
    IF candidate_count <> 1 THEN
        RAISE EXCEPTION '需要且仅能存在一个ont:ontology:list本体菜单，当前%个', candidate_count;
    END IF;
    FOR permission_record IN SELECT * FROM (VALUES
        ('ont:metric:execute', '执行受控指标查询'),
        ('ont:analysis:list', '本人分析列表'), ('ont:analysis:query', '本人分析详情'),
        ('ont:analysis:add', '新增本人分析'), ('ont:analysis:edit', '编辑本人分析'),
        ('ont:analysis:remove', '删除本人分析')
    ) AS permissions(perm, title)
    LOOP
        SELECT count(*), min(parent_id), min(menu_type) INTO candidate_count, existing_parent, existing_type
        FROM public.system_menu WHERE perms = permission_record.perm;
        IF candidate_count > 1 OR (candidate_count = 1 AND
                (existing_parent IS DISTINCT FROM ontology_menu_id OR existing_type IS DISTINCT FROM 'F')) THEN
            RAISE EXCEPTION '权限%已有重复或错误父菜单/类型，请人工核对', permission_record.perm;
        END IF;
        IF candidate_count = 0 THEN
            SELECT COALESCE(max(menu_id), 0) + 1 INTO next_id FROM public.system_menu;
            INSERT INTO public.system_menu
                (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                 menu_type, visible, status, perms, icon, create_by, create_time, remark)
            VALUES
                (next_id, permission_record.title, ontology_menu_id, 91, '#', NULL, 1, 0,
                 'F', '0', '0', permission_record.perm, '#', 'system', CURRENT_TIMESTAMP,
                 '默认不授权角色；请人工审核受控ADS来源、只读账号、空间成员及分析权限');
        END IF;
    END LOOP;
END $$;
COMMIT;
