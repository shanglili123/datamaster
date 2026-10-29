-- PostgreSQL. Apply manually after backup. Metadata definitions only; no source SQL is executed.
BEGIN;

CREATE TABLE public.ont_metric (
    id bigint PRIMARY KEY,
    ontology_id bigint NOT NULL REFERENCES public.ont_ontology(id) ON DELETE RESTRICT,
    concept_id bigint NOT NULL REFERENCES public.ont_concept(id) ON DELETE RESTRICT,
    concept_table_id bigint REFERENCES public.ont_concept_table(id) ON DELETE RESTRICT,
    code varchar(100) NOT NULL CHECK (code ~ '^[a-z][a-z0-9_]*$'),
    name varchar(200) NOT NULL,
    definition_json text NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'DISABLED')),
    published_version integer NOT NULL DEFAULT 0 CHECK (published_version >= 0),
    draft_changed boolean NOT NULL DEFAULT false,
    revision integer NOT NULL DEFAULT 0 CHECK (revision >= 0),
    creator_id bigint,
    create_by varchar(50),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updater_id bigint,
    update_by varchar(50),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ont_metric_ontology_code UNIQUE (ontology_id, code)
);
CREATE INDEX idx_ont_metric_concept ON public.ont_metric(concept_id);
CREATE INDEX idx_ont_metric_binding ON public.ont_metric(concept_table_id);
COMMENT ON TABLE public.ont_metric IS '本体指标当前工作定义；仅从未发布草稿可物理删除，删除后允许重用编码';

CREATE TABLE public.ont_metric_version (
    id bigint PRIMARY KEY,
    metric_id bigint NOT NULL REFERENCES public.ont_metric(id) ON DELETE RESTRICT,
    version integer NOT NULL CHECK (version > 0),
    snapshot_json text NOT NULL,
    creator_id bigint,
    create_by varchar(50),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updater_id bigint,
    update_by varchar(50),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ont_metric_version UNIQUE (metric_id, version)
);
COMMENT ON TABLE public.ont_metric_version IS '不可变发布快照：业务定义和实际物理映射；不代表数据已验证或已获源库授权';

CREATE FUNCTION public.reject_ont_metric_version_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '指标发布快照不可修改或删除';
END;
$$;
CREATE TRIGGER immutable_ont_metric_version
BEFORE UPDATE OR DELETE ON public.ont_metric_version
FOR EACH ROW EXECUTE FUNCTION public.reject_ont_metric_version_mutation();
CREATE TRIGGER immutable_ont_metric_version_truncate
BEFORE TRUNCATE ON public.ont_metric_version
FOR EACH STATEMENT EXECUTE FUNCTION public.reject_ont_metric_version_mutation();

-- Workbench tab, not a new top-level menu. Permission buttons are seeded under the
-- actual ontology menu. No role grants are made; an administrator reviews grants.
LOCK TABLE public.system_menu IN SHARE ROW EXCLUSIVE MODE;
DO $$
DECLARE
    ontology_menu_id bigint;
    candidate_count integer;
    next_id bigint;
    existing_parent bigint;
    permission_record record;
BEGIN
    SELECT count(*), min(menu_id) INTO candidate_count, ontology_menu_id
    FROM public.system_menu WHERE perms = 'ont:ontology:list' AND menu_type = 'C';
    IF candidate_count <> 1 THEN
        RAISE EXCEPTION '需要且仅能存在一个ont:ontology:list本体菜单，当前%个', candidate_count;
    END IF;
    FOR permission_record IN SELECT * FROM (VALUES
        ('ont:metric:list', '指标列表'), ('ont:metric:query', '指标详情与历史'),
        ('ont:metric:add', '新增指标'), ('ont:metric:edit', '编辑与校验指标'),
        ('ont:metric:remove', '删除未发布草稿'), ('ont:metric:publish', '发布与停用指标')
    ) AS permissions(perm, title)
    LOOP
        SELECT count(*), min(parent_id) INTO candidate_count, existing_parent
        FROM public.system_menu WHERE perms = permission_record.perm;
        IF candidate_count > 1 OR (candidate_count = 1 AND existing_parent <> ontology_menu_id) THEN
            RAISE EXCEPTION '权限%已有重复或错误父菜单，请人工核对', permission_record.perm;
        END IF;
        IF candidate_count = 0 THEN
            SELECT COALESCE(max(menu_id), 0) + 1 INTO next_id FROM public.system_menu;
            INSERT INTO public.system_menu
                (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                 menu_type, visible, status, perms, icon, create_by, create_time, remark)
            VALUES
                (next_id, permission_record.title, ontology_menu_id, 90, '#', NULL, 1, 0,
                 'F', '0', '0', permission_record.perm, '#', 'system', CURRENT_TIMESTAMP,
                 '本体工作台指标权限；请人工审核角色及空间授权');
        END IF;
    END LOOP;
END $$;
COMMIT;
