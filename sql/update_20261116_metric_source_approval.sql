-- PostgreSQL, manual migration. No role grants, source connections or legacy allowlist import.
BEGIN;
ALTER TABLE public.ont_concept_table ADD COLUMN source_auth_revision bigint NOT NULL DEFAULT 0 CHECK (source_auth_revision >= 0);
ALTER TABLE public.ast_datasource ADD COLUMN source_auth_revision bigint NOT NULL DEFAULT 0 CHECK (source_auth_revision >= 0);

CREATE FUNCTION public.advance_metric_binding_identity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN NEW.source_auth_revision := 0;
    ELSIF ROW(OLD.concept_id,OLD.datasource_id,OLD.database_name,OLD.schema_name,OLD.table_name,OLD.del_flag)
          IS DISTINCT FROM ROW(NEW.concept_id,NEW.datasource_id,NEW.database_name,NEW.schema_name,NEW.table_name,NEW.del_flag) THEN
        NEW.source_auth_revision := OLD.source_auth_revision + 1;
    ELSE NEW.source_auth_revision := OLD.source_auth_revision;
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER metric_binding_identity_revision BEFORE INSERT OR UPDATE ON public.ont_concept_table
FOR EACH ROW EXECUTE FUNCTION public.advance_metric_binding_identity();

CREATE FUNCTION public.advance_metric_datasource_identity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN NEW.source_auth_revision := 0;
    ELSIF ROW(OLD.datasource_type,OLD.ip,OLD.port,OLD.datasource_config,OLD.valid_flag,OLD.del_flag)
          IS DISTINCT FROM ROW(NEW.datasource_type,NEW.ip,NEW.port,NEW.datasource_config,NEW.valid_flag,NEW.del_flag) THEN
        NEW.source_auth_revision := OLD.source_auth_revision + 1;
    ELSE NEW.source_auth_revision := OLD.source_auth_revision;
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER metric_datasource_identity_revision BEFORE INSERT OR UPDATE ON public.ast_datasource
FOR EACH ROW EXECUTE FUNCTION public.advance_metric_datasource_identity();

CREATE TABLE public.ont_metric_source_approval (
    binding_id bigint PRIMARY KEY REFERENCES public.ont_concept_table(id) ON DELETE RESTRICT,
    ontology_id bigint NOT NULL REFERENCES public.ont_ontology(id) ON DELETE RESTRICT,
    space_id bigint NOT NULL,
    concept_id bigint NOT NULL REFERENCES public.ont_concept(id) ON DELETE RESTRICT,
    datasource_id bigint NOT NULL,
    binding_auth_revision bigint NOT NULL,
    datasource_auth_revision bigint NOT NULL,
    username varchar(128) NOT NULL,
    database_name varchar(100) NOT NULL,
    schema_name varchar(100),
    table_name varchar(100) NOT NULL,
    source_token varchar(64) NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('APPROVED','REVOKED')),
    revision integer NOT NULL CHECK (revision > 0),
    reason varchar(500) NOT NULL CHECK (length(btrim(reason)) > 0),
    approved_by bigint NOT NULL,
    approved_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint NOT NULL,
    updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_metric_source_approval_ontology ON public.ont_metric_source_approval(ontology_id,binding_id);
COMMENT ON TABLE public.ont_metric_source_approval IS '指标绑定的当前来源批准；管理员只读声明不是数据库权限验证；无密码或连接配置';

CREATE TABLE public.ont_metric_source_approval_event (
    id bigserial PRIMARY KEY,
    binding_id bigint NOT NULL REFERENCES public.ont_concept_table(id) ON DELETE RESTRICT,
    ontology_id bigint NOT NULL REFERENCES public.ont_ontology(id) ON DELETE RESTRICT,
    space_id bigint NOT NULL,
    action varchar(20) NOT NULL CHECK (action IN ('APPROVE','REVOKE')),
    revision integer NOT NULL CHECK (revision > 0),
    reason varchar(500) NOT NULL,
    actor_id bigint NOT NULL,
    occurred_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username varchar(128) NOT NULL,
    datasource_id bigint NOT NULL,
    database_name varchar(100) NOT NULL,
    schema_name varchar(100),
    table_name varchar(100) NOT NULL,
    UNIQUE(binding_id,revision)
);
CREATE INDEX idx_metric_source_approval_event ON public.ont_metric_source_approval_event(ontology_id,binding_id,id DESC);
CREATE FUNCTION public.reject_metric_source_event_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Metric source approval events are immutable'; END; $$;
CREATE TRIGGER immutable_metric_source_event BEFORE UPDATE OR DELETE ON public.ont_metric_source_approval_event
FOR EACH ROW EXECUTE FUNCTION public.reject_metric_source_event_mutation();
CREATE TRIGGER immutable_metric_source_event_truncate BEFORE TRUNCATE ON public.ont_metric_source_approval_event
FOR EACH STATEMENT EXECUTE FUNCTION public.reject_metric_source_event_mutation();

LOCK TABLE public.system_menu IN SHARE ROW EXCLUSIVE MODE;
DO $$
DECLARE parent bigint; n integer; next_id bigint; existing_parent bigint; existing_type varchar; p record;
BEGIN
    SELECT count(*),min(menu_id) INTO n,parent FROM public.system_menu WHERE perms='ont:ontology:list' AND menu_type='C';
    IF n <> 1 THEN RAISE EXCEPTION '需要唯一的 ont:ontology:list 本体菜单，当前 % 个', n; END IF;
    FOR p IN SELECT * FROM (VALUES
        ('ont:metric-source:query','指标查询来源配置'),
        ('ont:metric-source:approve','批准指标查询来源'),
        ('ont:metric-source:revoke','撤销指标查询来源')
    ) AS permissions(perm,title) LOOP
        SELECT count(*),min(parent_id),min(menu_type) INTO n,existing_parent,existing_type FROM public.system_menu WHERE perms=p.perm;
        IF n > 1 OR (n=1 AND (existing_parent IS DISTINCT FROM parent OR existing_type IS DISTINCT FROM 'F')) THEN
            RAISE EXCEPTION '权限 % 重复或父菜单/类型错误',p.perm;
        END IF;
        IF n=0 THEN
            SELECT COALESCE(max(menu_id),0)+1 INTO next_id FROM public.system_menu;
            INSERT INTO public.system_menu(menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
            VALUES(next_id,p.title,parent,92,'#',NULL,1,0,'F','0','0',p.perm,'#','system',CURRENT_TIMESTAMP,'默认不授权角色；仅空间管理员或平台管理员管理来源；不等于源库只读验证');
        END IF;
    END LOOP;
END; $$;
COMMIT;
