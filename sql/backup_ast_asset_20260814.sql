--
-- PostgreSQL database dump
--

-- Dumped from database version 15.8
-- Dumped by pg_dump version 15.8

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ast_asset; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset (
    id bigint NOT NULL,
    name character varying NOT NULL,
    cat_code character varying NOT NULL,
    datasource_id bigint NOT NULL,
    table_name character varying NOT NULL,
    table_comment character varying,
    data_count integer DEFAULT 0 NOT NULL,
    field_count integer DEFAULT 0 NOT NULL,
    status character(1) DEFAULT '1'::bpchar NOT NULL,
    description character varying,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    source character(1) DEFAULT '0'::bpchar NOT NULL,
    type character varying DEFAULT '1'::character varying NOT NULL,
    create_type character varying DEFAULT '2'::character varying,
    table_type character varying(10) DEFAULT '1'::character varying,
    data_layer_id bigint,
    business_category_id bigint,
    business_category_code character varying(256),
    data_domain_id bigint,
    theme_domain_id bigint,
    theme_domain_code character varying(256),
    table_case character varying(10),
    table_id bigint
);


ALTER TABLE public.ast_asset OWNER TO datamaster;

--
-- Name: TABLE ast_asset; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset IS 'ast_asset';


--
-- Name: ast_asset_apply; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_apply (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    space_id bigint NOT NULL,
    space_code character varying NOT NULL,
    apply_reason character varying,
    approval_reason character varying,
    status character(1) DEFAULT '1'::bpchar NOT NULL,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    source_type character varying DEFAULT '0'::character varying
);


ALTER TABLE public.ast_asset_apply OWNER TO datamaster;

--
-- Name: TABLE ast_asset_apply; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_apply IS 'ast_asset_apply';


--
-- Name: ast_asset_audit_alert; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_audit_alert (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    batch_no character varying NOT NULL,
    audit_time timestamp without time zone NOT NULL,
    alert_time timestamp without time zone,
    alert_message character varying,
    alert_channels character varying,
    alert_channel_result text,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone
);


ALTER TABLE public.ast_asset_audit_alert OWNER TO datamaster;

--
-- Name: TABLE ast_asset_audit_alert; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_audit_alert IS 'ast_asset_audit_alert';


--
-- Name: ast_asset_audit_rule; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_audit_rule (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    table_name character varying NOT NULL,
    column_name character varying NOT NULL,
    column_comment character varying,
    rule_name character varying,
    quality_dim character(1) NOT NULL,
    rule_type character(1) NOT NULL,
    rule_level character(1) DEFAULT '0'::bpchar NOT NULL,
    rule_description character varying,
    rule_config text NOT NULL,
    total_count bigint DEFAULT '0'::bigint,
    issue_count bigint DEFAULT '0'::bigint,
    audit_time timestamp without time zone NOT NULL,
    batch_no character varying NOT NULL,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    description character varying(256)
);


ALTER TABLE public.ast_asset_audit_rule OWNER TO datamaster;

--
-- Name: TABLE ast_asset_audit_rule; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_audit_rule IS 'ast_asset_audit_rule';


--
-- Name: ast_asset_audit_schedule; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_audit_schedule (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    schedule_flag character(1) DEFAULT NULL::bpchar,
    cron_expression character varying NOT NULL,
    node_id bigint NOT NULL,
    node_code character varying NOT NULL,
    task_id bigint NOT NULL,
    task_code character varying NOT NULL,
    system_job_id bigint,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone
);


ALTER TABLE public.ast_asset_audit_schedule OWNER TO datamaster;

--
-- Name: TABLE ast_asset_audit_schedule; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_audit_schedule IS 'ast_asset_audit_schedule';


--
-- Name: ast_asset_column; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_column (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    column_name character varying NOT NULL,
    column_comment character varying,
    column_type character varying NOT NULL,
    column_length integer,
    column_scale integer DEFAULT 0,
    nullable_flag character varying DEFAULT '0'::character varying NOT NULL,
    pk_flag character varying DEFAULT '0'::character varying NOT NULL,
    default_value character varying,
    data_elem_code_flag character varying DEFAULT '0'::character varying NOT NULL,
    data_elem_code_id bigint,
    sensitive_level_id bigint,
    rel_data_elme_flag character varying DEFAULT '0'::character varying NOT NULL,
    rel_clean_flag character varying DEFAULT '0'::character varying NOT NULL,
    rel_audit_flag character varying DEFAULT '0'::character varying NOT NULL,
    description character varying,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone
);


ALTER TABLE public.ast_asset_column OWNER TO datamaster;

--
-- Name: TABLE ast_asset_column; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_column IS 'ast_asset_column';


--
-- Name: ast_asset_column_space_rel; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_column_space_rel (
    id bigint NOT NULL,
    asset_id bigint,
    column_id bigint,
    space_id bigint,
    space_code character varying(256),
    valid_flag boolean DEFAULT true,
    del_flag boolean DEFAULT false,
    create_by character varying(64),
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying(64),
    updater_id bigint,
    update_time timestamp without time zone,
    description character varying(256)
);


ALTER TABLE public.ast_asset_column_space_rel OWNER TO datamaster;

--
-- Name: TABLE ast_asset_column_space_rel; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_column_space_rel IS '数据资产字段与项目关联关系表';


--
-- Name: COLUMN ast_asset_column_space_rel.asset_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.ast_asset_column_space_rel.asset_id IS '资产ID';


--
-- Name: COLUMN ast_asset_column_space_rel.column_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.ast_asset_column_space_rel.column_id IS '字段ID';


--
-- Name: COLUMN ast_asset_column_space_rel.space_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.ast_asset_column_space_rel.space_id IS '项目ID';


--
-- Name: COLUMN ast_asset_column_space_rel.space_code; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.ast_asset_column_space_rel.space_code IS '项目编码';


--
-- Name: ast_asset_file; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_file (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    file_source character varying NOT NULL,
    file_name character varying NOT NULL,
    file_url character varying NOT NULL,
    file_type character varying,
    file_size bigint NOT NULL,
    file_create_time character varying,
    file_update_time character varying,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    remark character varying
);


ALTER TABLE public.ast_asset_file OWNER TO datamaster;

--
-- Name: TABLE ast_asset_file; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_file IS 'ast_asset_file';


--
-- Name: ast_asset_files; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_files (
    id bigint NOT NULL,
    asset_id bigint,
    name character varying,
    url character varying NOT NULL,
    type character varying NOT NULL,
    start_data integer,
    start_column integer,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    remark character varying
);


ALTER TABLE public.ast_asset_files OWNER TO datamaster;

--
-- Name: TABLE ast_asset_files; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_files IS 'ast_asset_files';


--
-- Name: ast_asset_geo; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_geo (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    file_name character varying NOT NULL,
    file_url character varying NOT NULL,
    element_type character(1) NOT NULL,
    coordinate_system character varying NOT NULL,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    status character(1) DEFAULT '1'::bpchar,
    file_type character varying NOT NULL
);


ALTER TABLE public.ast_asset_geo OWNER TO datamaster;

--
-- Name: TABLE ast_asset_geo; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_geo IS 'ast_asset_geo';


--
-- Name: ast_asset_operate_apply; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_operate_apply (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    datasource_id bigint NOT NULL,
    table_name character varying NOT NULL,
    table_comment character varying,
    operate_type character(1) NOT NULL,
    operate_json text NOT NULL,
    operate_time timestamp without time zone NOT NULL,
    execute_flag character varying DEFAULT '0'::character varying NOT NULL,
    execute_time timestamp without time zone,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone
);


ALTER TABLE public.ast_asset_operate_apply OWNER TO datamaster;

--
-- Name: TABLE ast_asset_operate_apply; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_operate_apply IS 'ast_asset_operate_apply';


--
-- Name: ast_asset_operate_log; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_operate_log (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    datasource_id bigint NOT NULL,
    table_name character varying NOT NULL,
    table_comment character varying,
    operate_type character(1) NOT NULL,
    operate_time timestamp without time zone NOT NULL,
    execute_time timestamp without time zone NOT NULL,
    update_before text,
    update_after text,
    field_names character varying,
    file_url character varying,
    file_name character varying,
    status character(1) DEFAULT '1'::bpchar NOT NULL,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    update_where_md5 text
);


ALTER TABLE public.ast_asset_operate_log OWNER TO datamaster;

--
-- Name: TABLE ast_asset_operate_log; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_operate_log IS 'ast_asset_operate_log';


--
-- Name: ast_asset_space_rel; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_space_rel (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    space_id bigint NOT NULL,
    space_code character varying NOT NULL,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    description character varying(256)
);


ALTER TABLE public.ast_asset_space_rel OWNER TO datamaster;

--
-- Name: TABLE ast_asset_space_rel; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_space_rel IS 'ast_asset_project_rel';


--
-- Name: ast_asset_theme_rel; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_theme_rel (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    theme_id bigint NOT NULL,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone
);


ALTER TABLE public.ast_asset_theme_rel OWNER TO datamaster;

--
-- Name: TABLE ast_asset_theme_rel; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_theme_rel IS 'ast_asset_theme_rel';


--
-- Name: ast_asset_video; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.ast_asset_video (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    ip character varying NOT NULL,
    port integer NOT NULL,
    protocol character varying NOT NULL,
    platform character(1) DEFAULT '1'::bpchar NOT NULL,
    config character varying NOT NULL,
    status character(1) DEFAULT '1'::bpchar NOT NULL,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone
);


ALTER TABLE public.ast_asset_video OWNER TO datamaster;

--
-- Name: TABLE ast_asset_video; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.ast_asset_video IS 'ast_asset_video';


--
-- Name: std_data_elem_asset_rel; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.std_data_elem_asset_rel (
    id bigint NOT NULL,
    data_elem_type character(1) NOT NULL,
    data_elem_id bigint NOT NULL,
    asset_id bigint NOT NULL,
    table_name character varying NOT NULL,
    column_id bigint NOT NULL,
    column_name character varying NOT NULL,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    remark character varying,
    space_id bigint,
    space_code character varying
);


ALTER TABLE public.std_data_elem_asset_rel OWNER TO datamaster;

--
-- Name: TABLE std_data_elem_asset_rel; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.std_data_elem_asset_rel IS 'std_data_elem_asset_rel';


--
-- Name: COLUMN std_data_elem_asset_rel.space_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_data_elem_asset_rel.space_id IS '空间ID';


--
-- Name: COLUMN std_data_elem_asset_rel.space_code; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_data_elem_asset_rel.space_code IS '空间编码';


--
-- Name: std_desensitize_assetcolumn; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.std_desensitize_assetcolumn (
    id bigint NOT NULL,
    asset_id bigint,
    assetcolumn_id bigint,
    data_category_id bigint,
    sort_order bigint,
    description character varying(500),
    space_id bigint,
    space_code character varying(256),
    valid_flag boolean DEFAULT true,
    del_flag boolean DEFAULT false,
    create_by character varying(64),
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying(64),
    updater_id bigint,
    update_time timestamp without time zone,
    remark character varying(500)
);


ALTER TABLE public.std_desensitize_assetcolumn OWNER TO datamaster;

--
-- Name: TABLE std_desensitize_assetcolumn; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.std_desensitize_assetcolumn IS '脱敏清单关联关系表';


--
-- Name: COLUMN std_desensitize_assetcolumn.asset_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_desensitize_assetcolumn.asset_id IS '资产ID';


--
-- Name: COLUMN std_desensitize_assetcolumn.assetcolumn_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_desensitize_assetcolumn.assetcolumn_id IS '资产字段ID';


--
-- Name: COLUMN std_desensitize_assetcolumn.data_category_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_desensitize_assetcolumn.data_category_id IS '数据分类ID';


--
-- Name: COLUMN std_desensitize_assetcolumn.sort_order; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_desensitize_assetcolumn.sort_order IS '排序';


--
-- Name: COLUMN std_desensitize_assetcolumn.description; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_desensitize_assetcolumn.description IS '描述';


--
-- Name: COLUMN std_desensitize_assetcolumn.space_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_desensitize_assetcolumn.space_id IS '空间ID';


--
-- Name: COLUMN std_desensitize_assetcolumn.space_code; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_desensitize_assetcolumn.space_code IS '空间编码';


--
-- Name: std_model_materialized; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.std_model_materialized (
    id bigint NOT NULL,
    model_name character varying NOT NULL,
    model_alias character varying NOT NULL,
    model_id bigint NOT NULL,
    status character(1) DEFAULT '0'::bpchar NOT NULL,
    message text,
    sql_command text,
    datasource_id bigint NOT NULL,
    datasource_type character varying NOT NULL,
    datasource_name character varying NOT NULL,
    asset_id bigint,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    remark character varying,
    space_id bigint,
    space_code character varying
);


ALTER TABLE public.std_model_materialized OWNER TO datamaster;

--
-- Name: TABLE std_model_materialized; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.std_model_materialized IS 'std_model_materialized';


--
-- Name: COLUMN std_model_materialized.space_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_model_materialized.space_id IS '空间ID';


--
-- Name: COLUMN std_model_materialized.space_code; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.std_model_materialized.space_code IS '空间编码';


--
-- Name: tax_tag_asset_rel; Type: TABLE; Schema: public; Owner: datamaster
--

CREATE TABLE public.tax_tag_asset_rel (
    id bigint NOT NULL,
    tag_id bigint,
    asset_id bigint,
    valid_flag character varying DEFAULT '1'::character varying NOT NULL,
    del_flag character varying DEFAULT '0'::character varying NOT NULL,
    create_by character varying,
    creator_id bigint,
    create_time timestamp without time zone,
    update_by character varying,
    updater_id bigint,
    update_time timestamp without time zone,
    remark character varying,
    space_id bigint,
    space_code character varying
);


ALTER TABLE public.tax_tag_asset_rel OWNER TO datamaster;

--
-- Name: TABLE tax_tag_asset_rel; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON TABLE public.tax_tag_asset_rel IS 'tax_tag_asset_rel';


--
-- Name: COLUMN tax_tag_asset_rel.space_id; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.tax_tag_asset_rel.space_id IS '项目ID';


--
-- Name: COLUMN tax_tag_asset_rel.space_code; Type: COMMENT; Schema: public; Owner: datamaster
--

COMMENT ON COLUMN public.tax_tag_asset_rel.space_code IS '项目编码';


--
-- Data for Name: ast_asset; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset (id, name, cat_code, datasource_id, table_name, table_comment, data_count, field_count, status, description, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, source, type, create_type, table_type, data_layer_id, business_category_id, business_category_code, data_domain_id, theme_domain_id, theme_domain_code, table_case, table_id) FROM stdin;
2084264527225970689	行政区划字典	A02	8	ADMIN_REGION_DICT	行政区划字典	0	7	1	\N	true	0	\N	\N	2026-08-03 21:06:01.717	\N	\N	2026-08-14 23:42:00.725	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264527477628930	站点设备台账	A02	8	DEVICE	站点设备台账	0	9	1	\N	true	0	\N	\N	2026-08-03 21:06:01.775	\N	\N	2026-08-14 23:42:00.768	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264527729287171	水位原始明细（读源）	A02	8	WATER_LEVEL	水位原始明细（读源）	6000	6	1	\N	true	0	\N	\N	2026-08-03 21:06:01.844	\N	\N	2026-08-14 23:42:00.819	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264527918030851	水位预警事件	A02	8	WARNING_EVENT	水位预警事件	0	9	1	\N	true	0	\N	\N	2026-08-03 21:06:01.887	\N	\N	2026-08-14 23:42:00.854	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264528232603650	站点水位预警阈值（版本区间）	A02	8	STATION_THRESHOLD	站点水位预警阈值（版本区间）	0	8	1	\N	true	0	\N	\N	2026-08-03 21:06:01.954	\N	\N	2026-08-14 23:42:00.901	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264528614285313	站点扩展属性（键值对/JSON）	A02	8	STATION_ATTR	站点扩展属性（键值对/JSON）	0	7	1	\N	true	0	\N	\N	2026-08-03 21:06:02.045	\N	\N	2026-08-14 23:42:00.938	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264528861749249	站点基础信息（读源）	A02	8	STATION	站点基础信息（读源）	504	11	1	\N	true	0	\N	\N	2026-08-03 21:06:02.103	\N	\N	2026-08-14 23:42:00.968	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264529239236612	水库水情明细（位/容/入出流）	A02	8	RESERVOIR_LEVEL	水库水情明细（位/容/入出流）	0	9	1	\N	true	0	\N	\N	2026-08-03 21:06:02.204	\N	\N	2026-08-14 23:42:01.01	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264529553809411	降雨观测明细	A02	8	RAINFALL	降雨观测明细	0	6	1	\N	true	0	\N	\N	2026-08-03 21:06:02.279	\N	\N	2026-08-14 23:42:01.055	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264529805467649	流量观测明细	A02	8	DISCHARGE	流量观测明细	0	6	1	\N	true	0	\N	\N	2026-08-03 21:06:02.33	\N	\N	2026-08-14 23:42:01.091	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264525841850370	传感器/数据源	A01	7	hyd_sensor	传感器/数据源	999	10	1	\N	true	0	\N	\N	2026-08-03 21:06:01.389	\N	\N	2026-08-14 23:42:00.512	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264526156423170	降雨量时序数据	A01	7	hyd_rainfall	降雨量时序数据	999	12	1	\N	true	0	\N	\N	2026-08-03 21:06:01.459	\N	\N	2026-08-14 23:42:00.559	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264526470995970	监测数据质量代码字典	A01	7	hyd_quality_code	监测数据质量代码字典	999	4	1	\N	true	0	\N	\N	2026-08-03 21:06:01.539	\N	\N	2026-08-14 23:42:00.611	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264526659739650	流量时序数据	A01	7	hyd_discharge	流量时序数据	999	11	1	\N	true	0	\N	\N	2026-08-03 21:06:01.58	\N	\N	2026-08-14 23:42:00.634	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264527037227010	流域字典	A02	8	BASIN_DICT	流域字典	0	7	1	\N	true	0	\N	\N	2026-08-03 21:06:01.668	\N	\N	2026-08-14 23:42:00.693	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264520875794433	告警事件实施表	A03	5	fact_alarm_event	告警事件实施表	1800	14	1	\N	true	0	\N	\N	2026-08-03 21:06:00.197	\N	\N	2026-08-14 23:43:00.02	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264522255720450	养护/维修实施表	A03	5	fact_maintenance	养护/维修实施表	1500	14	1	\N	true	0	\N	\N	2026-08-03 21:06:00.536	\N	\N	2026-08-14 23:43:00.091	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2077290202182811650	accc	A03	9	test_orders		0	9	2		1	0	超级管理员	1	2026-07-15 15:12:33.051	超级管理员	1	2026-07-15 15:12:33.051	3	1	2	1	\N	\N	\N	\N	\N	\N	\N	2069240120863854593
2077301190944628738	ssss	A03	9	test_customer_order	DataMaster PostgreSQL测试订单表	5	8	2		1	0	超级管理员	1	2026-07-15 15:56:12.97	超级管理员	1	2026-07-15 16:36:59.945	3	1	2	1	\N	\N	\N	\N	\N	\N	\N	2073363300481085441
2084264522759036930	组织维表	A03	5	dim_org	组织维表	150	10	1	\N	true	0	\N	\N	2026-08-03 21:06:00.658	\N	\N	2026-08-14 23:43:00.187	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264523136524291	巡检实施表	A03	5	fact_inspection	巡检实施表	2000	13	1	\N	true	0	\N	\N	2026-08-03 21:06:00.747	\N	\N	2026-08-14 23:43:00.241	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264523514011650	项目维表	A03	5	dim_project	项目维表	300	10	1	\N	true	0	\N	\N	2026-08-03 21:06:00.839	\N	\N	2026-08-14 23:43:00.307	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264523891499010	运行日志实施表	A03	5	fact_operation_log	运行日志实施表	2500	11	1	\N	true	0	\N	\N	2026-08-03 21:06:00.922	\N	\N	2026-08-14 23:43:00.366	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264524268986371	工程资产维表	A03	5	dim_asset	工程资产维表	1000	16	1	\N	true	0	\N	\N	2026-08-03 21:06:01.017	\N	\N	2026-08-14 23:43:00.428	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264524776497154	在线监测/遥测事实表	A03	5	fact_telemetry	在线监测/遥测事实表	3000	10	1	\N	true	0	\N	\N	2026-08-03 21:06:01.127	\N	\N	2026-08-14 23:43:00.507	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264525149790210	水位时序数据	A01	7	hyd_water_level	水位时序数据	999	11	1	\N	true	0	\N	\N	2026-08-03 21:06:01.217	\N	\N	2026-08-14 23:43:00.599	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
2084264525464363009	水文站点基础信息	A01	7	hyd_station	水文站点基础信息	999	14	1	\N	true	0	\N	\N	2026-08-03 21:06:01.292	\N	\N	2026-08-14 23:42:00.457	1	1	2	1	\N	\N	\N	\N	\N	\N	\N	\N
\.


--
-- Data for Name: ast_asset_apply; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_apply (id, asset_id, space_id, space_code, apply_reason, approval_reason, status, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, source_type) FROM stdin;
2077301192316166145	2077301190944628738	1	174954643786848	新增数据资产	\N	3	1	0	超级管理员	1	2026-07-15 15:56:13	超级管理员	1	2026-07-15 16:34:34.446	0
\.


--
-- Data for Name: ast_asset_audit_alert; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_audit_alert (id, asset_id, batch_no, audit_time, alert_time, alert_message, alert_channels, alert_channel_result, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time) FROM stdin;
\.


--
-- Data for Name: ast_asset_audit_rule; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_audit_rule (id, asset_id, table_name, column_name, column_comment, rule_name, quality_dim, rule_type, rule_level, rule_description, rule_config, total_count, issue_count, audit_time, batch_no, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, description) FROM stdin;
\.


--
-- Data for Name: ast_asset_audit_schedule; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_audit_schedule (id, asset_id, schedule_flag, cron_expression, node_id, node_code, task_id, task_code, system_job_id, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time) FROM stdin;
\.


--
-- Data for Name: ast_asset_column; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_column (id, asset_id, column_name, column_comment, column_type, column_length, column_scale, nullable_flag, pk_flag, default_value, data_elem_code_flag, data_elem_code_id, sensitive_level_id, rel_data_elme_flag, rel_clean_flag, rel_audit_flag, description, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time) FROM stdin;
2084264521874038785	2084264520875794433	handler	处理人	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.436	\N	\N	2026-08-03 21:06:00.436
2084264522696122372	2084264522255720450	asset_id	资产ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.643	\N	\N	2026-08-03 21:06:00.643
2084264523325267972	2084264523136524291	insp_user	巡检人/算法	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.794	\N	\N	2026-08-03 21:06:00.794
2084264523388182530	2084264523136524291	issue_desc	问题描述	varchar	2147483643	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.799	\N	\N	2026-08-03 21:06:00.799
2084264523388182531	2084264523136524291	issue_level	问题等级	tinyint	3	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.804	\N	\N	2026-08-03 21:06:00.804
2084264523388182532	2084264523136524291	insp_type	巡检类型	varchar	50	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.809	\N	\N	2026-08-03 21:06:00.809
2084264523451097089	2084264523136524291	asset_code	资产编码	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.814	\N	\N	2026-08-03 21:06:00.814
2084264523451097091	2084264523136524291	asset_id	资产ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.824	\N	\N	2026-08-03 21:06:00.824
2084264523639840769	2084264523514011650	update_time	记录更新时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.857	\N	\N	2026-08-03 21:06:00.857
2084264523639840770	2084264523514011650	ext_json	扩展信息（JSON）	varchar	2147483643	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.862	\N	\N	2026-08-03 21:06:00.862
2084264523639840771	2084264523514011650	status	状态：1在建/2完工/0停缓建	tinyint	3	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.866	\N	\N	2026-08-03 21:06:00.866
2084264523702755329	2084264523514011650	end_date	完工/验收日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.871	\N	\N	2026-08-03 21:06:00.871
2084264523702755330	2084264523514011650	start_date	开工日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.877	\N	\N	2026-08-03 21:06:00.877
2084264523702755331	2084264523514011650	owner_org_id	业主单位ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.883	\N	\N	2026-08-03 21:06:00.883
2084264523765669889	2084264523514011650	project_name	项目名称	varchar	200	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.89	\N	\N	2026-08-03 21:06:00.89
2084264523765669890	2084264523514011650	project_code	项目编码	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.896	\N	\N	2026-08-03 21:06:00.896
2084264521936953346	2084264520875794433	clear_time	清除时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.452	\N	\N	2026-08-03 21:06:00.452
2084264521936953347	2084264520875794433	raise_time	触发时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.462	\N	\N	2026-08-03 21:06:00.462
2084264523828584450	2084264523514011650	dt	快照日期	date	0	0	1	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.903	\N	\N	2026-08-03 21:06:00.903
2084264523828584451	2084264523514011650	project_id	项目ID	bigint	19	0	1	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.91	\N	\N	2026-08-03 21:06:00.91
2084264524017328129	2084264523891499010	ts	写入时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.946	\N	\N	2026-08-03 21:06:00.946
2084264524017328130	2084264523891499010	event_time	事件时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.952	\N	\N	2026-08-03 21:06:00.952
2084264524017328131	2084264523891499010	operator	操作人	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.958	\N	\N	2026-08-03 21:06:00.958
2084264524080242689	2084264523891499010	op_content	事件内容	varchar	2147483643	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.964	\N	\N	2026-08-03 21:06:00.964
2084264524080242690	2084264523891499010	op_type	事件类型	varchar	50	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.97	\N	\N	2026-08-03 21:06:00.97
2084264524143157250	2084264523891499010	project_id	项目ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.982	\N	\N	2026-08-03 21:06:00.982
2084264524143157251	2084264523891499010	asset_code	资产编码	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.988	\N	\N	2026-08-03 21:06:00.988
2084264524206071809	2084264523891499010	op_id	运行事件ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.996	\N	\N	2026-08-03 21:06:00.996
2084264524457730049	2084264524268986371	build_year	建成年份	int	10	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.053	\N	\N	2026-08-03 21:06:01.053
2084264524457730050	2084264524268986371	design_level	设计标准：防洪标准/保证率等	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.058	\N	\N	2026-08-03 21:06:01.058
2084264524457730051	2084264524268986371	latitude	纬度	double	15	15	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.063	\N	\N	2026-08-03 21:06:01.063
2084264522759036929	2084264522255720450	dt	分区日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.649	\N	\N	2026-08-03 21:06:00.649
2084264522884866049	2084264522759036930	update_time	记录更新时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.683	\N	\N	2026-08-03 21:06:00.683
2084264522884866050	2084264522759036930	status	状态：1启用/0停用	tinyint	3	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.689	\N	\N	2026-08-03 21:06:00.689
2084264524520644609	2084264524268986371	longitude	经度	double	15	15	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.068	\N	\N	2026-08-03 21:06:01.068
2084264522960363522	2084264522759036930	contact_phone	联系电话	varchar	50	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.697	\N	\N	2026-08-03 21:06:00.697
2084264522960363523	2084264522759036930	contact_name	联系人	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.703	\N	\N	2026-08-03 21:06:00.703
2084264523010695170	2084264522759036930	admin_region	行政区划	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.71	\N	\N	2026-08-03 21:06:00.71
2084264524520644610	2084264524268986371	admin_region	行政区划（代码或名称）	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.073	\N	\N	2026-08-03 21:06:01.073
2084264524520644611	2084264524268986371	river_name	河流/渠道名称	varchar	128	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.079	\N	\N	2026-08-03 21:06:01.079
2084264524583559169	2084264524268986371	org_id	运维单位ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.084	\N	\N	2026-08-03 21:06:01.084
2084264523010695171	2084264522759036930	org_type	类型：OWNER/OPERATOR/CONTRACTOR等	varchar	50	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.716	\N	\N	2026-08-03 21:06:00.716
2084264523073609729	2084264522759036930	org_name	组织名称	varchar	200	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.721	\N	\N	2026-08-03 21:06:00.721
2084264523073609730	2084264522759036930	org_code	组织编码	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.727	\N	\N	2026-08-03 21:06:00.727
2084264523073609731	2084264522759036930	dt	快照日期	date	0	0	1	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.732	\N	\N	2026-08-03 21:06:00.732
2084264523136524290	2084264522759036930	org_id	组织ID	bigint	19	0	1	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.737	\N	\N	2026-08-03 21:06:00.737
2084264523262353410	2084264523136524291	ts	写入时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.768	\N	\N	2026-08-03 21:06:00.768
2084264523262353411	2084264523136524291	media_urls	照片/视频URL	varchar	2147483643	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.773	\N	\N	2026-08-03 21:06:00.773
2084264523262353412	2084264523136524291	geo_lat	纬度	double	15	15	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.778	\N	\N	2026-08-03 21:06:00.778
2084264523325267970	2084264523136524291	geo_long	经度	double	15	15	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.784	\N	\N	2026-08-03 21:06:00.784
2084264523325267971	2084264523136524291	insp_time	巡检时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.789	\N	\N	2026-08-03 21:06:00.789
2084264521874038786	2084264520875794433	state	状态：0未处理/1处理中/2已解除/3忽略	tinyint	3	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.445	\N	\N	2026-08-03 21:06:00.445
2084264523451097090	2084264523136524291	insp_id	巡检ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.819	\N	\N	2026-08-03 21:06:00.819
2084264525967679490	2084264525841850370	created_at	\N	timestamp	0	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.412	\N	\N	2026-08-03 21:06:01.412
2084264525967679491	2084264525841850370	ext_json	扩展参数（原 JSON）	text	65535	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.417	\N	\N	2026-08-03 21:06:01.417
2084264527100141569	2084264527037227010	CREATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.684	\N	\N	2026-08-03 21:06:01.684
2084264527100141570	2084264527037227010	STATUS	1启用/0停用	NUMBER	1	0	0	0	1	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.689	\N	\N	2026-08-03 21:06:01.689
2084264527351799810	2084264527225970689	STATUS	1启用/0停用	NUMBER	1	0	0	0	1	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.744	\N	\N	2026-08-03 21:06:01.744
2084264527351799811	2084264527225970689	LEVEL_NO	层级	NUMBER	2	0	0	0	1	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.749	\N	\N	2026-08-03 21:06:01.749
2084264522129891330	2084264520875794433	level	等级：1一般/2较重/3严重/4特别严重	tinyint	3	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.496	\N	\N	2026-08-03 21:06:00.496
2084264522129891331	2084264520875794433	alarm_type	告警类别	varchar	50	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.504	\N	\N	2026-08-03 21:06:00.504
2084264525212704770	2084264525149790210	created_at	\N	timestamp	0	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.234	\N	\N	2026-08-03 21:06:01.234
2084264525212704771	2084264525149790210	ext_json	扩展字段（原 JSON）	text	65535	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.24	\N	\N	2026-08-03 21:06:01.24
2084264525338533893	2084264525149790210	sensor_id	传感器ID（软关联）	bigint	20	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.273	\N	\N	2026-08-03 21:06:01.273
2084264527792201730	2084264527729287171	TS	源端变更时间	TIMESTAMP	8	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.858	\N	\N	2026-08-03 21:06:01.858
2084264527855116289	2084264527729287171	QUALITY_FLAG	源端质控标识	VARCHAR2	16	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.864	\N	\N	2026-08-03 21:06:01.864
2084264524583559170	2084264524268986371	project_id	所属项目ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.089	\N	\N	2026-08-03 21:06:01.089
2084264524646473729	2084264524268986371	asset_type	资产类型：RESERVOIR/LEVEE/PUMP_STATION/SLUICE等	varchar	50	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.095	\N	\N	2026-08-03 21:06:01.095
2084264525967679492	2084264525841850370	status	1启用/0停用	tinyint	3	0	1	0	1	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.422	\N	\N	2026-08-03 21:06:01.422
2084264526785568770	2084264526659739650	trace_id	追踪ID	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.608	\N	\N	2026-08-03 21:06:01.608
2084264527100141571	2084264527037227010	LEVEL_NO	层级	NUMBER	2	0	0	0	1	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.694	\N	\N	2026-08-03 21:06:01.694
2084264527163056130	2084264527037227010	PARENT_CODE	上级流域编码	VARCHAR2	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.7	\N	\N	2026-08-03 21:06:01.7
2084264527855116291	2084264527729287171	OBS_TIME	观测时间	TIMESTAMP	8	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.872	\N	\N	2026-08-03 21:06:01.872
2084264527918030849	2084264527729287171	STATION_CODE	站点编码	VARCHAR2	64	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.877	\N	\N	2026-08-03 21:06:01.877
2084264524709388290	2084264524268986371	dt	快照日期	date	0	0	1	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.111	\N	\N	2026-08-03 21:06:01.111
2084264524961046529	2084264524776497154	asset_code	资产编码	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.171	\N	\N	2026-08-03 21:06:01.171
2084264525275619330	2084264525149790210	trace_id	追踪ID/批次号	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.246	\N	\N	2026-08-03 21:06:01.246
2084264525275619331	2084264525149790210	source	来源	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.25	\N	\N	2026-08-03 21:06:01.25
2084264525716021252	2084264525464363009	station_type	站点类型	enum	9	0	1	0	COMPOSITE	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.363	\N	\N	2026-08-03 21:06:01.363
2084264526785568771	2084264526659739650	source	来源	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.613	\N	\N	2026-08-03 21:06:01.613
2084264526911397890	2084264526659739650	station_code	站点编码（软关联）	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.64	\N	\N	2026-08-03 21:06:01.64
2084264527163056131	2084264527037227010	BASIN_NAME	流域名称	VARCHAR2	200	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.704	\N	\N	2026-08-03 21:06:01.704
2084264527163056132	2084264527037227010	BASIN_CODE	流域编码	VARCHAR2	64	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.709	\N	\N	2026-08-03 21:06:01.709
2084264525275619332	2084264525149790210	quality_code	质量代码	varchar	16	0	1	0	OK	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.255	\N	\N	2026-08-03 21:06:01.255
2084264525338533890	2084264525149790210	water_level_m	水位(米)	decimal	10	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.26	\N	\N	2026-08-03 21:06:01.26
2084264525778935810	2084264525464363009	station_name	站点名称	varchar	200	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.37	\N	\N	2026-08-03 21:06:01.37
2084264525778935811	2084264525464363009	station_code	站点编码（第三方库唯一）	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.375	\N	\N	2026-08-03 21:06:01.375
2084264525778935812	2084264525464363009	station_id	站点主键	bigint	20	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.379	\N	\N	2026-08-03 21:06:01.379
2084264525904764930	2084264525841850370	updated_at	\N	timestamp	0	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.406	\N	\N	2026-08-03 21:06:01.406
2084264526911397891	2084264526659739650	id	自增主键	bigint	20	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.644	\N	\N	2026-08-03 21:06:01.644
2084264527037227011	2084264527037227010	UPDATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.679	\N	\N	2026-08-03 21:06:01.679
2084264527288885250	2084264527225970689	UPDATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.733	\N	\N	2026-08-03 21:06:01.733
2084264527288885251	2084264527225970689	CREATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.738	\N	\N	2026-08-03 21:06:01.738
2084264522062782466	2084264520875794433	detail	告警详情	varchar	2147483643	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.481	\N	\N	2026-08-03 21:06:00.481
2084264522062782467	2084264520875794433	title	告警标题	varchar	200	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.489	\N	\N	2026-08-03 21:06:00.489
2084264524961046530	2084264524776497154	row_id	行ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.177	\N	\N	2026-08-03 21:06:01.177
2084264525023961091	2084264524776497154	dt	分区日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.198	\N	\N	2026-08-03 21:06:01.198
2084264525338533891	2084264525149790210	obs_date	观测日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.264	\N	\N	2026-08-03 21:06:01.264
2084264525338533892	2084264525149790210	obs_time	观测时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.268	\N	\N	2026-08-03 21:06:01.268
2084264528798834690	2084264528614285313	ATTR_KEY	属性键	VARCHAR2	64	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.089	\N	\N	2026-08-03 21:06:02.089
2084264528798834691	2084264528614285313	STATION_CODE	站点编码	VARCHAR2	64	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.095	\N	\N	2026-08-03 21:06:02.095
2084264528924663809	2084264528861749249	UPDATED_AT	修改时间	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.12	\N	\N	2026-08-03 21:06:02.12
2084264528924663810	2084264528861749249	CREATED_AT	创建时间	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.126	\N	\N	2026-08-03 21:06:02.126
2084264528987578369	2084264528861749249	STATUS	1启用/0停用	NUMBER	1	0	0	0	1	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.131	\N	\N	2026-08-03 21:06:02.131
2084264529050492930	2084264528861749249	ADMIN_REGION_CODE	行政区划码	VARCHAR2	12	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.149	\N	\N	2026-08-03 21:06:02.149
2084264529050492931	2084264528861749249	LATITUDE	纬度	DECIMAL	10	6	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.159	\N	\N	2026-08-03 21:06:02.159
2084264529113407490	2084264528861749249	LONGITUDE	经度	DECIMAL	10	6	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.166	\N	\N	2026-08-03 21:06:02.166
2084264528106774532	2084264527918030851	START_TIME	\N	TIMESTAMP	8	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.934	\N	\N	2026-08-03 21:06:01.934
2084264528169689090	2084264527918030851	STATION_CODE	\N	VARCHAR2	64	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.94	\N	\N	2026-08-03 21:06:01.94
2084264528169689091	2084264527918030851	EVENT_ID	\N	NUMBER	19	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.945	\N	\N	2026-08-03 21:06:01.945
2084264528295518209	2084264528232603650	UPDATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.979	\N	\N	2026-08-03 21:06:01.979
2084264528358432769	2084264528232603650	CREATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.986	\N	\N	2026-08-03 21:06:01.986
2084264528358432770	2084264528232603650	LEVEL_RED	\N	DECIMAL	10	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.992	\N	\N	2026-08-03 21:06:01.992
2084264528358432771	2084264528232603650	LEVEL_YELLOW	\N	DECIMAL	10	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.997	\N	\N	2026-08-03 21:06:01.997
2084264528438124545	2084264528232603650	LEVEL_BLUE	\N	DECIMAL	10	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.009	\N	\N	2026-08-03 21:06:02.009
2084264526785568772	2084264526659739650	quality_code	质量代码	varchar	16	0	1	0	OK	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.618	\N	\N	2026-08-03 21:06:01.618
2084264528492650498	2084264528232603650	EFFECTIVE_TO	生效止	TIMESTAMP	8	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.015	\N	\N	2026-08-03 21:06:02.015
2084264522188611585	2084264520875794433	asset_code	资产编码	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.511	\N	\N	2026-08-03 21:06:00.511
2084264526848483330	2084264526659739650	discharge_m3s	流量(m^3/s)	decimal	12	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.622	\N	\N	2026-08-03 21:06:01.622
2084264526848483331	2084264526659739650	obs_date	观测日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.627	\N	\N	2026-08-03 21:06:01.627
2084264526848483332	2084264526659739650	obs_time	观测时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.631	\N	\N	2026-08-03 21:06:01.631
2084264526911397889	2084264526659739650	sensor_id	传感器ID（软关联）	bigint	20	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.635	\N	\N	2026-08-03 21:06:01.635
2084264527918030850	2084264527729287171	ID	记录主键（源）	NUMBER	19	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.88	\N	\N	2026-08-03 21:06:01.88
2084264527980945409	2084264527918030851	UPDATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.901	\N	\N	2026-08-03 21:06:01.901
2084264528043859969	2084264527918030851	CREATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.908	\N	\N	2026-08-03 21:06:01.908
2084264528492650499	2084264528232603650	EFFECTIVE_FROM	生效起	TIMESTAMP	8	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.023	\N	\N	2026-08-03 21:06:02.023
2084264528551370753	2084264528232603650	STATION_CODE	\N	VARCHAR2	64	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.035	\N	\N	2026-08-03 21:06:02.035
2084264528673005569	2084264528614285313	UPDATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.064	\N	\N	2026-08-03 21:06:02.064
2084264528673005570	2084264528614285313	CREATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.069	\N	\N	2026-08-03 21:06:02.069
2084264528735920129	2084264528614285313	REMARK	\N	VARCHAR2	500	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.074	\N	\N	2026-08-03 21:06:02.074
2084264528735920130	2084264528614285313	VALUE_TYPE	值类型	VARCHAR2	16	0	0	0	'STRING'	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.08	\N	\N	2026-08-03 21:06:02.08
2084264528735920131	2084264528614285313	ATTR_VALUE	属性值	VARCHAR2	4000	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.084	\N	\N	2026-08-03 21:06:02.084
2084264522197000194	2084264520875794433	asset_id	资产ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.517	\N	\N	2026-08-03 21:06:00.517
2084264522197000195	2084264520875794433	alarm_id	告警ID	bigint	19	0	1	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.523	\N	\N	2026-08-03 21:06:00.523
2084264522389938178	2084264522255720450	ts	写入时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.565	\N	\N	2026-08-03 21:06:00.565
2084264522444464130	2084264522255720450	status	状态	tinyint	3	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.579	\N	\N	2026-08-03 21:06:00.579
2084264522507378690	2084264522255720450	contractor_org	承包单位	varchar	200	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.586	\N	\N	2026-08-03 21:06:00.586
2084264522507378691	2084264522255720450	cost_amount	费用（元）	decimal	18	2	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.592	\N	\N	2026-08-03 21:06:00.592
2084264528106774530	2084264527918030851	WARNING_LEVEL	3红/2黄/1蓝/0无	NUMBER	1	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.924	\N	\N	2026-08-03 21:06:01.924
2084264526722654210	2084264526659739650	created_at	\N	timestamp	0	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.597	\N	\N	2026-08-03 21:06:01.597
2084264526722654211	2084264526659739650	ext_json	扩展（原 JSON）	text	65535	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.602	\N	\N	2026-08-03 21:06:01.602
2084264526030594049	2084264525841850370	precision_scale	仪器精度	decimal	4	2	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.428	\N	\N	2026-08-03 21:06:01.428
2084264526030594050	2084264525841850370	unit	单位：m/m3/s/mm	varchar	32	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.432	\N	\N	2026-08-03 21:06:01.432
2084264521811124226	2084264520875794433	dt	分区日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.421	\N	\N	2026-08-03 21:06:00.421
2084264521811124227	2084264520875794433	ts	写入时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.429	\N	\N	2026-08-03 21:06:00.429
2084264521999867905	2084264520875794433	source	来源	varchar	50	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.47	\N	\N	2026-08-03 21:06:00.47
2084264522507378692	2084264522255720450	actual_end	实际结束	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.598	\N	\N	2026-08-03 21:06:00.598
2084264522570293250	2084264522255720450	actual_start	实际开始	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.604	\N	\N	2026-08-03 21:06:00.604
2084264522570293251	2084264522255720450	plan_end	计划结束	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.61	\N	\N	2026-08-03 21:06:00.61
2084264522633207810	2084264522255720450	plan_start	计划开始	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.615	\N	\N	2026-08-03 21:06:00.615
2084264522633207811	2084264522255720450	work_desc	作业内容	varchar	2147483643	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.621	\N	\N	2026-08-03 21:06:00.621
2084264522696122370	2084264522255720450	asset_code	资产编码	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.632	\N	\N	2026-08-03 21:06:00.632
2084264522696122371	2084264522255720450	work_id	作业单ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.637	\N	\N	2026-08-03 21:06:00.637
2084264526030594051	2084264525841850370	metric_type	监测指标类型	enum	11	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.437	\N	\N	2026-08-03 21:06:01.437
2084264526219337731	2084264526156423170	ext_json	扩展（原 JSON）	text	65535	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.479	\N	\N	2026-08-03 21:06:01.479
2084264526219337732	2084264526156423170	trace_id	追踪ID	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.484	\N	\N	2026-08-03 21:06:01.484
2084264526282252290	2084264526156423170	source	来源	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.488	\N	\N	2026-08-03 21:06:01.488
2084264526282252291	2084264526156423170	quality_code	质量代码	varchar	16	0	1	0	OK	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.493	\N	\N	2026-08-03 21:06:01.493
2084264526282252292	2084264526156423170	rainfall_mm	降雨量(毫米)	decimal	10	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.498	\N	\N	2026-08-03 21:06:01.498
2084264526345166849	2084264526156423170	period	统计周期	enum	6	0	1	0	HOUR	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.503	\N	\N	2026-08-03 21:06:01.503
2084264526345166850	2084264526156423170	obs_date	观测日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.508	\N	\N	2026-08-03 21:06:01.508
2084264526408081410	2084264526156423170	sensor_id	传感器ID（软关联）	bigint	20	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.519	\N	\N	2026-08-03 21:06:01.519
2084264526408081411	2084264526156423170	station_code	站点编码（软关联）	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.525	\N	\N	2026-08-03 21:06:01.525
2084264526470995969	2084264526156423170	id	自增主键	bigint	20	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.53	\N	\N	2026-08-03 21:06:01.53
2084264526533910530	2084264526470995970	created_at	\N	timestamp	0	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.555	\N	\N	2026-08-03 21:06:01.555
2084264526596825090	2084264526470995970	severity	0正常，1提示，2可疑，3错误	tinyint	3	0	1	0	0	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.561	\N	\N	2026-08-03 21:06:01.561
2084264526596825091	2084264526470995970	quality_desc	质量说明	varchar	200	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.566	\N	\N	2026-08-03 21:06:01.566
2084264526596825092	2084264526470995970	quality_code	质量代码	varchar	16	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.571	\N	\N	2026-08-03 21:06:01.571
2084264524646473730	2084264524268986371	asset_name	资产名称	varchar	200	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.101	\N	\N	2026-08-03 21:06:01.101
2084264524646473731	2084264524268986371	asset_code	资产编码	varchar	100	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.106	\N	\N	2026-08-03 21:06:01.106
2084264524709388291	2084264524268986371	asset_id	资产ID（唯一标识）	bigint	19	0	1	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.117	\N	\N	2026-08-03 21:06:01.117
2084264524839411713	2084264524776497154	ts	写入时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.148	\N	\N	2026-08-03 21:06:01.148
2084264524839411714	2084264524776497154	quality	质量：0正常/1估算/2缺测/3异常	tinyint	3	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.153	\N	\N	2026-08-03 21:06:01.153
2084264524898131970	2084264524776497154	unit	单位	varchar	32	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.16	\N	\N	2026-08-03 21:06:01.16
2084264524898131971	2084264524776497154	value	值	double	15	15	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.165	\N	\N	2026-08-03 21:06:01.165
2084264524961046531	2084264524776497154	event_time	采集时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.182	\N	\N	2026-08-03 21:06:01.182
2084264525023961089	2084264524776497154	metric	指标	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.187	\N	\N	2026-08-03 21:06:01.187
2084264525023961090	2084264524776497154	asset_id	资产ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.193	\N	\N	2026-08-03 21:06:01.193
2084264525401448450	2084264525149790210	station_code	站点编码（软关联）	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.278	\N	\N	2026-08-03 21:06:01.278
2084264525401448451	2084264525149790210	id	自增主键	bigint	20	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.282	\N	\N	2026-08-03 21:06:01.282
2084264525527277569	2084264525464363009	updated_at	\N	timestamp	0	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.309	\N	\N	2026-08-03 21:06:01.309
2084264525527277570	2084264525464363009	created_at	\N	timestamp	0	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.314	\N	\N	2026-08-03 21:06:01.314
2084264525590192130	2084264525464363009	ext_json	扩展字段（原 JSON）	text	65535	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.32	\N	\N	2026-08-03 21:06:01.32
2084264525590192131	2084264525464363009	status	1启用/0停用	tinyint	3	0	1	0	1	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.326	\N	\N	2026-08-03 21:06:01.326
2084264525590192132	2084264525464363009	elevation_m	站点高程(米)	decimal	8	2	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.331	\N	\N	2026-08-03 21:06:01.331
2084264525653106690	2084264525464363009	latitude	纬度	decimal	10	6	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.343	\N	\N	2026-08-03 21:06:01.343
2084264525653106691	2084264525464363009	longitude	经度	decimal	10	6	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.347	\N	\N	2026-08-03 21:06:01.347
2084264525716021250	2084264525464363009	river_name	河流名称	varchar	128	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.353	\N	\N	2026-08-03 21:06:01.353
2084264525716021251	2084264525464363009	basin_code	流域编码	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.359	\N	\N	2026-08-03 21:06:01.359
2084264523514011649	2084264523136524291	dt	分区日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.83	\N	\N	2026-08-03 21:06:00.83
2084264524206071810	2084264523891499010	asset_id	资产ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.002	\N	\N	2026-08-03 21:06:01.002
2084264524268986370	2084264523891499010	dt	分区日期	date	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.008	\N	\N	2026-08-03 21:06:01.008
2084264524394815489	2084264524268986371	update_time	记录更新时间	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.036	\N	\N	2026-08-03 21:06:01.036
2084264524394815490	2084264524268986371	ext_json	扩展信息（JSON文本）	varchar	2147483643	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.042	\N	\N	2026-08-03 21:06:01.042
2084264526093508610	2084264525841850370	sensor_code	传感器/接口编码	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.442	\N	\N	2026-08-03 21:06:01.442
2084264526093508611	2084264525841850370	station_code	所属站点编码（软关联）	varchar	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.446	\N	\N	2026-08-03 21:06:01.446
2084264526093508612	2084264525841850370	sensor_id	传感器主键	bigint	20	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.451	\N	\N	2026-08-03 21:06:01.452
2084264526219337730	2084264526156423170	created_at	\N	timestamp	0	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.475	\N	\N	2026-08-03 21:06:01.475
2084264527351799812	2084264527225970689	PARENT_CODE	上级行政区划码	VARCHAR2	12	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.754	\N	\N	2026-08-03 21:06:01.754
2084264527414714369	2084264527225970689	REGION_NAME	行政区划名称	VARCHAR2	200	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.761	\N	\N	2026-08-03 21:06:01.761
2084264527414714370	2084264527225970689	REGION_CODE	行政区划码	VARCHAR2	12	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.766	\N	\N	2026-08-03 21:06:01.766
2084264527540543489	2084264527477628930	UPDATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.793	\N	\N	2026-08-03 21:06:01.793
2084264527540543490	2084264527477628930	CREATED_AT	\N	TIMESTAMP	8	0	0	0	CURRENT_TIMESTAMP	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.798	\N	\N	2026-08-03 21:06:01.798
2084264527603458049	2084264527477628930	STATUS	1在用/0停用	NUMBER	1	0	0	0	1	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.804	\N	\N	2026-08-03 21:06:01.804
2084264527603458050	2084264527477628930	INSTALL_TIME	安装时间	TIMESTAMP	8	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.81	\N	\N	2026-08-03 21:06:01.81
2084264527603458051	2084264527477628930	SERIAL_NO	序列号	VARCHAR2	128	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.814	\N	\N	2026-08-03 21:06:01.814
2084264527666372611	2084264527477628930	DEVICE_TYPE	设备类型	VARCHAR2	64	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.825	\N	\N	2026-08-03 21:06:01.825
2084264527729287169	2084264527477628930	STATION_CODE	所属站点编码	VARCHAR2	64	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.831	\N	\N	2026-08-03 21:06:01.831
2084264527729287170	2084264527477628930	DEVICE_ID	设备ID	NUMBER	19	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.836	\N	\N	2026-08-03 21:06:01.836
2084264527855116290	2084264527729287171	WATER_LEVEL	水位（米）	DECIMAL	10	3	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.868	\N	\N	2026-08-03 21:06:01.868
2084264528043859970	2084264527918030851	STATUS	事件状态：active/cleared	VARCHAR2	16	0	0	0	'active'	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.913	\N	\N	2026-08-03 21:06:01.913
2077290202245726209	2077290202182811650	id		INTEGER	4	0	1	1	nextval('test_orders_id_seq'::regclass)	0	\N	\N	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:12:33.064	超级管理员	1	2026-07-15 15:12:33.064
2077290202308640770	2077290202182811650	user_id		INTEGER	4	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:12:33.087	超级管理员	1	2026-07-15 15:12:33.087
2077290202438664193	2077290202182811650	order_no		VARCHAR	50	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:12:33.106	超级管理员	1	2026-07-15 15:12:33.106
2077290202568687618	2077290202182811650	quantity		INTEGER	4	0	0	0	1	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:12:33.136	超级管理员	1	2026-07-15 15:12:33.136
2077290202631602178	2077290202182811650	amount		numeric	12	2	0	0	\N	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:12:33.15	超级管理员	1	2026-07-15 15:12:33.15
2077290202694516738	2077290202182811650	status		VARCHAR	20	0	0	0	'pending'::character varying	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:12:33.168	超级管理员	1	2026-07-15 15:12:33.168
2077290202694516740	2077290202182811650	order_date		date	4	0	0	0	CURRENT_DATE	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:12:33.179	超级管理员	1	2026-07-15 15:12:33.179
2077290202761625603	2077290202182811650	created_at		timestamp	8	0	0	0	CURRENT_TIMESTAMP	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:12:33.193	超级管理员	1	2026-07-15 15:12:33.193
2077301191108206593	2077301190944628738	id		BIGINT	8	0	1	1	nextval('test_customer_order_id_seq'::regclass)	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:56:13.017	超级管理员	1	2026-07-15 15:56:13.017
2077301191364059139	2077301190944628738	order_no	订单编号	VARCHAR	32	0	1	0	\N	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:56:13.083	超级管理员	1	2026-07-15 15:56:13.083
2077301191494082561	2077301190944628738	customer_name	客户名称	VARCHAR	64	0	1	0	\N	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:56:13.102	超级管理员	1	2026-07-15 15:56:13.102
2077301191556997121	2077301190944628738	city		VARCHAR	64	0	0	0	\N	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:56:13.117	超级管理员	1	2026-07-15 15:56:13.117
2077301191619911683	2077301190944628738	amount	订单金额	numeric	12	2	1	0	\N	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:56:13.135	超级管理员	1	2026-07-15 15:56:13.135
2084264528043859971	2084264527918030851	MAX_LEVEL	\N	DECIMAL	10	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.917	\N	\N	2026-08-03 21:06:01.917
2084264528106774531	2084264527918030851	END_TIME	\N	TIMESTAMP	8	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.929	\N	\N	2026-08-03 21:06:01.929
2084264529113407491	2084264528861749249	RIVER_NAME	河流名称	VARCHAR2	128	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.173	\N	\N	2026-08-03 21:06:02.173
2084264529176322050	2084264528861749249	BASIN_CODE	流域编码	VARCHAR2	64	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.179	\N	\N	2026-08-03 21:06:02.179
2084264529176322051	2084264528861749249	STATION_NAME	站点名称	VARCHAR2	200	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.185	\N	\N	2026-08-03 21:06:02.185
2084264529239236611	2084264528861749249	STATION_ID	站点主键（源）	NUMBER	19	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.196	\N	\N	2026-08-03 21:06:02.196
2084264529365065730	2084264529239236612	TS	\N	TIMESTAMP	8	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.222	\N	\N	2026-08-03 21:06:02.222
2084264529365065731	2084264529239236612	QUALITY_FLAG	\N	VARCHAR2	16	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.228	\N	\N	2026-08-03 21:06:02.228
2084264529365065732	2084264529239236612	INFLOW_CMS	\N	DECIMAL	12	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.233	\N	\N	2026-08-03 21:06:02.233
2084264529427980290	2084264529239236612	OUTFLOW_CMS	\N	DECIMAL	12	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.239	\N	\N	2026-08-03 21:06:02.239
2084264529427980291	2084264529239236612	STORAGE_MCM	库容（百万立方米）	DECIMAL	14	3	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.245	\N	\N	2026-08-03 21:06:02.245
2084264529490894850	2084264529239236612	WATER_LEVEL	\N	DECIMAL	10	3	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.251	\N	\N	2026-08-03 21:06:02.251
2084264529490894851	2084264529239236612	OBS_TIME	\N	TIMESTAMP	8	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.258	\N	\N	2026-08-03 21:06:02.258
2084264529490894852	2084264529239236612	STATION_CODE	\N	VARCHAR2	64	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.263	\N	\N	2026-08-03 21:06:02.263
2084264524143157249	2084264523891499010	org_id	运维单位ID	bigint	19	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.976	\N	\N	2026-08-03 21:06:00.976
2077301191682826241	2077301190944628738	order_status		VARCHAR	20	0	1	0	\N	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:56:13.152	超级管理员	1	2026-07-15 15:56:13.152
2077301191745740802	2077301190944628738	order_time		timestamp	8	0	1	0	\N	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:56:13.172	超级管理员	1	2026-07-15 15:56:13.172
2077301191884152834	2077301190944628738	created_at		timestamp	8	0	1	0	CURRENT_TIMESTAMP	0	\N	5	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:56:13.196	超级管理员	1	2026-07-15 15:56:13.196
2077290202501578754	2077290202182811650	product_name		VARCHAR	200	0	0	0	\N	0	\N	1	0	0	0	\N	1	0	超级管理员	1	2026-07-15 15:12:33	超级管理员	1	2026-07-15 17:48:09.059
2084264525653106689	2084264525464363009	admin_region_code	行政区划码	varchar	12	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.337	\N	\N	2026-08-03 21:06:01.337
2084264526345166851	2084264526156423170	obs_time	统计时间(周期结束时刻)	timestamp(6)	0	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.514	\N	\N	2026-08-03 21:06:01.514
2084264527666372610	2084264527477628930	DEVICE_MODEL	设备型号	VARCHAR2	128	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.82	\N	\N	2026-08-03 21:06:01.82
2084264529239236610	2084264528861749249	STATION_CODE	站点编码（跨源统一关联键）	VARCHAR2	64	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.191	\N	\N	2026-08-03 21:06:02.191
2084264529553809410	2084264529239236612	ID	\N	NUMBER	19	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.27	\N	\N	2026-08-03 21:06:02.27
2084264529679638529	2084264529553809411	TS	\N	TIMESTAMP	8	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.295	\N	\N	2026-08-03 21:06:02.295
2084264529679638530	2084264529553809411	QUALITY_FLAG	\N	VARCHAR2	16	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.3	\N	\N	2026-08-03 21:06:02.3
2084264529679638531	2084264529553809411	PRECIP_MM	降雨量（毫米）	DECIMAL	10	2	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.305	\N	\N	2026-08-03 21:06:02.305
2084264529679638532	2084264529553809411	OBS_TIME	\N	TIMESTAMP	8	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.31	\N	\N	2026-08-03 21:06:02.31
2084264529755136001	2084264529553809411	STATION_CODE	\N	VARCHAR2	64	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.316	\N	\N	2026-08-03 21:06:02.316
2084264529755136002	2084264529553809411	ID	\N	NUMBER	19	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.322	\N	\N	2026-08-03 21:06:02.322
2084264529868382209	2084264529805467649	TS	\N	TIMESTAMP	8	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.346	\N	\N	2026-08-03 21:06:02.346
2084264529868382210	2084264529805467649	QUALITY_FLAG	\N	VARCHAR2	16	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.352	\N	\N	2026-08-03 21:06:02.352
2084264529931296769	2084264529805467649	DISCHARGE_CMS	流量（立方米/秒）	DECIMAL	12	3	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.357	\N	\N	2026-08-03 21:06:02.357
2084264529931296770	2084264529805467649	OBS_TIME	\N	TIMESTAMP	8	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.364	\N	\N	2026-08-03 21:06:02.364
2084264529931296771	2084264529805467649	STATION_CODE	\N	VARCHAR2	64	0	0	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.368	\N	\N	2026-08-03 21:06:02.368
2084264529994211330	2084264529805467649	ID	\N	NUMBER	19	0	0	1	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:02.375	\N	\N	2026-08-03 21:06:02.375
2084264522633207812	2084264522255720450	work_type	作业类型	varchar	50	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:00.626	\N	\N	2026-08-03 21:06:00.626
2084264524394815491	2084264524268986371	status	状态：1启用/0停用/2在建/3停运	tinyint	3	0	1	0	\N	0	\N	\N	0	0	0	\N	1	0	\N	\N	2026-08-03 21:06:01.047	\N	\N	2026-08-03 21:06:01.047
\.


--
-- Data for Name: ast_asset_column_space_rel; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_column_space_rel (id, asset_id, column_id, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, description) FROM stdin;
2077290202308640769	2077290202182811650	2077290202245726209	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:12:33.077	超级管理员	1	2026-07-15 15:12:33.077	\N
2077290202384138241	2077290202182811650	2077290202308640770	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:12:33.1	超级管理员	1	2026-07-15 15:12:33.1	\N
2077290202438664194	2077290202182811650	2077290202438664193	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:12:33.116	超级管理员	1	2026-07-15 15:12:33.116	\N
2077290202501578755	2077290202182811650	2077290202501578754	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:12:33.13	超级管理员	1	2026-07-15 15:12:33.13	\N
2077290202568687619	2077290202182811650	2077290202568687618	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:12:33.144	超级管理员	1	2026-07-15 15:12:33.144	\N
2077290202631602179	2077290202182811650	2077290202631602178	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:12:33.16	超级管理员	1	2026-07-15 15:12:33.16	\N
2077290202694516739	2077290202182811650	2077290202694516738	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:12:33.175	超级管理员	1	2026-07-15 15:12:33.175	\N
2077290202761625602	2077290202182811650	2077290202694516740	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:12:33.188	超级管理员	1	2026-07-15 15:12:33.188	\N
2077290202824540161	2077290202182811650	2077290202761625603	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:12:33.201	超级管理员	1	2026-07-15 15:12:33.201	\N
2077301191364059138	2077301190944628738	2077301191108206593	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:56:13.076	超级管理员	1	2026-07-15 15:56:13.076	\N
2077301191431168002	2077301190944628738	2077301191364059139	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:56:13.096	超级管理员	1	2026-07-15 15:56:13.096	\N
2077301191494082562	2077301190944628738	2077301191494082561	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:56:13.111	超级管理员	1	2026-07-15 15:56:13.111	\N
2077301191619911682	2077301190944628738	2077301191556997121	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:56:13.129	超级管理员	1	2026-07-15 15:56:13.129	\N
2077301191619911684	2077301190944628738	2077301191619911683	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:56:13.143	超级管理员	1	2026-07-15 15:56:13.143	\N
2077301191745740801	2077301190944628738	2077301191682826241	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:56:13.164	超级管理员	1	2026-07-15 15:56:13.164	\N
2077301191812849665	2077301190944628738	2077301191745740802	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:56:13.185	超级管理员	1	2026-07-15 15:56:13.185	\N
2077301191938678786	2077301190944628738	2077301191884152834	1	174954643786848	t	f	超级管理员	1	2026-07-15 15:56:13.209	超级管理员	1	2026-07-15 15:56:13.209	\N
\.


--
-- Data for Name: ast_asset_file; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_file (id, asset_id, file_source, file_name, file_url, file_type, file_size, file_create_time, file_update_time, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, remark) FROM stdin;
\.


--
-- Data for Name: ast_asset_files; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_files (id, asset_id, name, url, type, start_data, start_column, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, remark) FROM stdin;
\.


--
-- Data for Name: ast_asset_geo; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_geo (id, asset_id, file_name, file_url, element_type, coordinate_system, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, status, file_type) FROM stdin;
\.


--
-- Data for Name: ast_asset_operate_apply; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_operate_apply (id, asset_id, datasource_id, table_name, table_comment, operate_type, operate_json, operate_time, execute_flag, execute_time, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time) FROM stdin;
\.


--
-- Data for Name: ast_asset_operate_log; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_operate_log (id, asset_id, datasource_id, table_name, table_comment, operate_type, operate_time, execute_time, update_before, update_after, field_names, file_url, file_name, status, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, update_where_md5) FROM stdin;
\.


--
-- Data for Name: ast_asset_space_rel; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_space_rel (id, asset_id, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, description) FROM stdin;
2077290202887454722	2077290202182811650	1	174954643786848	1	0	超级管理员	1	2026-07-15 15:12:33.219	超级管理员	1	2026-07-15 15:12:33.219	\N
2077301192001593346	2077301190944628738	1	174954643786848	1	1	超级管理员	1	2026-07-15 15:56:13.233	超级管理员	1	2026-07-15 15:56:13.233	\N
2077301218945802242	2077301190944628738	1	174954643786848	1	0	超级管理员	1	2026-07-15 15:56:19.652	超级管理员	1	2026-07-15 15:56:19.652	\N
\.


--
-- Data for Name: ast_asset_theme_rel; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_theme_rel (id, asset_id, theme_id, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time) FROM stdin;
2077290203013283842	2077290202182811650	2	1	0	超级管理员	1	2026-07-15 15:12:33.251	超级管理员	1	2026-07-15 15:12:33.251
2077301192190337025	2077301190944628738	3	1	0	超级管理员	1	2026-07-15 15:56:13.278	超级管理员	1	2026-07-15 15:56:13.278
\.


--
-- Data for Name: ast_asset_video; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.ast_asset_video (id, asset_id, ip, port, protocol, platform, config, status, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time) FROM stdin;
\.


--
-- Data for Name: std_data_elem_asset_rel; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.std_data_elem_asset_rel (id, data_elem_type, data_elem_id, asset_id, table_name, column_id, column_name, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, remark, space_id, space_code) FROM stdin;
1	1	1	26	WR_RV_B	176	RVCD	1	0	test	2	2025-09-22 19:27:05	test	2	2025-09-22 19:27:05	\N	1	174954643786848
2	1	2	26	WR_RV_B	177	RVNM	1	0	test	2	2025-09-22 19:27:05	test	2	2025-09-22 19:27:05	\N	1	174954643786848
3	1	3	26	WR_RV_B	178	RVPL	1	0	test	2	2025-09-22 19:27:05	test	2	2025-09-22 19:27:05	\N	1	174954643786848
4	1	4	26	WR_RV_B	179	USER_ID	1	0	test	2	2025-09-22 19:27:05	test	2	2025-09-22 19:27:05	\N	1	174954643786848
5	1	5	26	WR_RV_B	180	LVBSLV	1	0	test	2	2025-09-22 19:27:05	test	2	2025-09-22 19:27:05	\N	1	174954643786848
6	1	6	26	WR_RV_B	181	RVSREL	1	0	test	2	2025-09-22 19:27:05	test	2	2025-09-22 19:27:05	\N	1	174954643786848
7	1	7	26	WR_RV_B	182	RVTRPL	1	0	test	2	2025-09-22 19:27:05	test	2	2025-09-22 19:27:05	\N	1	174954643786848
8	1	28	27	ST_RSVRFSR_B	183	BGMD	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
9	1	29	27	ST_RSVRFSR_B	184	EDMD	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
10	1	30	27	ST_RSVRFSR_B	185	FSLTDZ	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
11	1	31	27	ST_RSVRFSR_B	186	FSLTDW	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
12	1	32	27	ST_RSVRFSR_B	187	FSTP	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
13	1	17	28	SWC_QS_JLCJBQKB	188	JLCBM	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
14	1	18	28	SWC_QS_JLCJBQKB	189	JLCMC	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
15	1	19	28	SWC_QS_JLCJBQKB	190	JCDDM	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
16	1	20	28	SWC_QS_JLCJBQKB	191	JLJRQ	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
17	1	21	28	SWC_QS_JLCJBQKB	192	JLCWZ	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
18	1	22	28	SWC_QS_JLCJBQKB	193	SBPZ	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
19	1	23	28	SWC_QS_JLCJBQKB	194	GCXM	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
20	1	24	28	SWC_QS_JLCJBQKB	195	PW	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
21	1	25	28	SWC_QS_JLCJBQKB	196	JYZL	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
22	1	26	28	SWC_QS_JLCJBQKB	197	TRLXDM	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
23	1	27	28	SWC_QS_JLCJBQKB	198	YJZHL	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
24	1	8	29	ST_PPTN_R	199	STCD	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
25	1	33	29	ST_PPTN_R	200	TM	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
26	1	34	29	ST_PPTN_R	201	DRP	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
27	1	35	29	ST_PPTN_R	202	INTV	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
28	1	37	29	ST_PPTN_R	203	DYP	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
29	1	38	29	ST_PPTN_R	204	WTH	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
\.


--
-- Data for Name: std_desensitize_assetcolumn; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.std_desensitize_assetcolumn (id, asset_id, assetcolumn_id, data_category_id, sort_order, description, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, remark) FROM stdin;
\.


--
-- Data for Name: std_model_materialized; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.std_model_materialized (id, model_name, model_alias, model_id, status, message, sql_command, datasource_id, datasource_type, datasource_name, asset_id, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, remark, space_id, space_code) FROM stdin;
2	WR_RV_B	河流基本信息表	2	4	表 [WR_RV_B] 已存在，无需重复创建	\N	12	DM8	水资源管理系统（第三方库）	\N	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
1	WR_RV_B	河流基本信息表	2	3	建表成功	[CREATE TABLE WATER_TP.WR_RV_B (\\n  RVCD VARCHAR2(1024),\\n  RVNM VARCHAR2(1024),\\n  RVPL VARCHAR2(1024),\\n  USER_ID VARCHAR2(1024),\\n  LVBSLV VARCHAR2(1024),\\n  RVSREL VARCHAR2(1024),\\n  RVTRPL VARCHAR2(1024)\\n), COMMENT ON TABLE WATER_TP.WR_RV_B IS '河流基本信息表', COMMENT ON COLUMN WATER_TP.WR_RV_B.RVCD IS '河流代码', COMMENT ON COLUMN WATER_TP.WR_RV_B.RVNM IS '河流名称', COMMENT ON COLUMN WATER_TP.WR_RV_B.RVPL IS '河源位置', COMMENT ON COLUMN WATER_TP.WR_RV_B.USER_ID IS '用水户编码', COMMENT ON COLUMN WATER_TP.WR_RV_B.LVBSLV IS '水准基面', COMMENT ON COLUMN WATER_TP.WR_RV_B.RVSREL IS '河源高程', COMMENT ON COLUMN WATER_TP.WR_RV_B.RVTRPL IS '河口位置']	12	DM8	水资源管理系统（第三方库）	26	1	0	test	2	2025-09-22 19:27:05	test	2	2025-09-22 19:27:05	\N	1	174954643786848
3	ST_RVFCCH_B	河道站防洪指标表	3	4	建表失败：StatementCallback; SQL [CREATE TABLE WATER_TP.ST_RVFCCH_B (\\n  STCD VARCHAR2(1),\\n  STCD VARCHAR2(1024),\\n  LDKEL VARCHAR2(1024),\\n  RDKEL VARCHAR2(1024),\\n  WRZ VARCHAR2(1024),\\n  WRQ VARCHAR2(1024),\\n  GRZ VARCHAR2(1024),\\n  GRQ VARCHAR2(1024),\\n  GRQ VARCHAR2(1024),\\n  OBHTZ VARCHAR2(1024),\\n  IVHZ VARCHAR2(1024)\\n)]; Error in line: 13\\nColumn [STCD] already exists; nested exception is dm.jdbc.driver.DMException: Error in line: 13\\nColumn [STCD] already exists	[CREATE TABLE WATER_TP.ST_RVFCCH_B (\\n  STCD VARCHAR2(1),\\n  STCD VARCHAR2(1024),\\n  LDKEL VARCHAR2(1024),\\n  RDKEL VARCHAR2(1024),\\n  WRZ VARCHAR2(1024),\\n  WRQ VARCHAR2(1024),\\n  GRZ VARCHAR2(1024),\\n  GRQ VARCHAR2(1024),\\n  GRQ VARCHAR2(1024),\\n  OBHTZ VARCHAR2(1024),\\n  IVHZ VARCHAR2(1024)\\n), COMMENT ON TABLE WATER_TP.ST_RVFCCH_B IS '河道站防洪指标表', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.STCD IS '测站编码', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.STCD IS '测站编码', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.LDKEL IS '左堤高程', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.RDKEL IS '右堤高程', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.WRZ IS '警戒水位', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.WRQ IS '警戒流量', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.GRZ IS '保证水位', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.GRQ IS '保证流量', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.GRQ IS '保证流量', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.OBHTZ IS '实测最高水位', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.IVHZ IS '调查最高水']	12	DM8	水资源管理系统（第三方库）	\N	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
4	ST_RSVRFSR_B	汛限水位表	4	3	建表成功	[CREATE TABLE WATER_TP.ST_RSVRFSR_B (\\n  BGMD VARCHAR2(526),\\n  EDMD VARCHAR2(526),\\n  FSLTDZ VARCHAR2(526),\\n  FSLTDW VARCHAR2(526),\\n  FSTP VARCHAR2(526)\\n), COMMENT ON TABLE WATER_TP.ST_RSVRFSR_B IS '汛限水位表', COMMENT ON COLUMN WATER_TP.ST_RSVRFSR_B.BGMD IS '开始月日', COMMENT ON COLUMN WATER_TP.ST_RSVRFSR_B.EDMD IS '结束月日', COMMENT ON COLUMN WATER_TP.ST_RSVRFSR_B.FSLTDZ IS '汛限水位', COMMENT ON COLUMN WATER_TP.ST_RSVRFSR_B.FSLTDW IS '汛限库容', COMMENT ON COLUMN WATER_TP.ST_RSVRFSR_B.FSTP IS '汛期类别']	12	DM8	水资源管理系统（第三方库）	27	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
5	SWC_QS_JLCJBQKB	径流场基本情况表	5	3	建表成功	[CREATE TABLE WATER_TP.SWC_QS_JLCJBQKB (\\n  JLCBM VARCHAR2(526),\\n  JLCMC VARCHAR2(526),\\n  JCDDM VARCHAR2(526),\\n  JLJRQ  DATETIME,\\n  JLCWZ VARCHAR2(526),\\n  SBPZ VARCHAR2(526),\\n  GCXM VARCHAR2(526),\\n  PW VARCHAR2(526),\\n  JYZL VARCHAR2(526),\\n  TRLXDM VARCHAR2(526),\\n  YJZHL VARCHAR2(526)\\n), COMMENT ON TABLE WATER_TP.SWC_QS_JLCJBQKB IS '径流场基本情况表', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.JLCBM IS '径流场编码', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.JLCMC IS '径流场名称', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.JCDDM IS '监测点代码', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.JLJRQ IS '建立日期', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.JLCWZ IS '径流场位置', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.SBPZ IS '设备配置', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.GCXM IS '观测项目', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.PW IS '坡位', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.JYZL IS '基岩种类', COMMENT ON COLUMN WATER_TP.SWC_QS_JLCJBQKB.TRLXDM IS '土壤类型代码', COMMENT ON COLUMN WATER...	12	DM8	水资源管理系统（第三方库）	28	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
6	ST_PPTN_R	降水量表	6	3	建表成功	[CREATE TABLE WATER_TP.ST_PPTN_R (\\n  STCD VARCHAR2(526),\\n  TM  DATETIME,\\n  DRP VARCHAR2(526),\\n  INTV VARCHAR2(526),\\n  DYP VARCHAR2(526),\\n  WTH VARCHAR2(526)\\n), COMMENT ON TABLE WATER_TP.ST_PPTN_R IS '降水量表', COMMENT ON COLUMN WATER_TP.ST_PPTN_R.STCD IS '测站编码', COMMENT ON COLUMN WATER_TP.ST_PPTN_R.TM IS '时间', COMMENT ON COLUMN WATER_TP.ST_PPTN_R.DRP IS '时段降水量', COMMENT ON COLUMN WATER_TP.ST_PPTN_R.INTV IS '时段长', COMMENT ON COLUMN WATER_TP.ST_PPTN_R.DYP IS '日降水量', COMMENT ON COLUMN WATER_TP.ST_PPTN_R.WTH IS '天气状况']	12	DM8	水资源管理系统（第三方库）	29	1	0	test	2	2025-09-22 19:28:12	test	2	2025-09-22 19:28:12	\N	1	174954643786848
7	ST_RVFCCH_B	河道站防洪指标表	3	4	建表失败：StatementCallback; SQL [CREATE TABLE WATER_TP.ST_RVFCCH_B (\\n  STCD VARCHAR2(1024),\\n  STCD VARCHAR2(1024),\\n  LDKEL VARCHAR2(1024),\\n  RDKEL VARCHAR2(1024),\\n  WRZ VARCHAR2(1024),\\n  WRQ VARCHAR2(1024),\\n  GRZ VARCHAR2(1024),\\n  GRQ VARCHAR2(1024),\\n  GRQ VARCHAR2(1024),\\n  OBHTZ VARCHAR2(1024),\\n  IVHZ VARCHAR2(1024)\\n)]; Error in line: 13\\nColumn [STCD] already exists; nested exception is dm.jdbc.driver.DMException: Error in line: 13\\nColumn [STCD] already exists	[CREATE TABLE WATER_TP.ST_RVFCCH_B (\\n  STCD VARCHAR2(1024),\\n  STCD VARCHAR2(1024),\\n  LDKEL VARCHAR2(1024),\\n  RDKEL VARCHAR2(1024),\\n  WRZ VARCHAR2(1024),\\n  WRQ VARCHAR2(1024),\\n  GRZ VARCHAR2(1024),\\n  GRQ VARCHAR2(1024),\\n  GRQ VARCHAR2(1024),\\n  OBHTZ VARCHAR2(1024),\\n  IVHZ VARCHAR2(1024)\\n), COMMENT ON TABLE WATER_TP.ST_RVFCCH_B IS '河道站防洪指标表', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.STCD IS '测站编码', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.STCD IS '测站编码', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.LDKEL IS '左堤高程', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.RDKEL IS '右堤高程', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.WRZ IS '警戒水位', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.WRQ IS '警戒流量', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.GRZ IS '保证水位', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.GRQ IS '保证流量', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.GRQ IS '保证流量', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.OBHTZ IS '实测最高水位', COMMENT ON COLUMN WATER_TP.ST_RVFCCH_B.IVHZ IS '调查最高水']	12	DM8	水资源管理系统（第三方库）	\N	1	0	test	2	2025-09-22 19:29:42	test	2	2025-09-22 19:29:42	\N	1	174954643786848
\.


--
-- Data for Name: tax_tag_asset_rel; Type: TABLE DATA; Schema: public; Owner: datamaster
--

COPY public.tax_tag_asset_rel (id, tag_id, asset_id, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time, remark, space_id, space_code) FROM stdin;
1	1	20	1	0	超级管理员	1	2025-09-22 16:18:37	超级管理员	1	2025-09-22 16:18:37	\N	1	174954643786848
2	1	19	1	0	超级管理员	1	2025-09-22 16:19:04	超级管理员	1	2025-09-22 16:19:04	\N	1	174954643786848
3	1	18	1	0	超级管理员	1	2025-09-22 16:19:20	超级管理员	1	2025-09-22 16:19:20	\N	1	174954643786848
4	1	17	1	0	超级管理员	1	2025-09-22 16:19:32	超级管理员	1	2025-09-22 16:19:32	\N	1	174954643786848
5	1	16	1	0	超级管理员	1	2025-09-22 16:19:44	超级管理员	1	2025-09-22 16:19:44	\N	1	174954643786848
6	1	1	1	0	超级管理员	1	2025-09-22 16:19:58	超级管理员	1	2025-09-22 16:19:58	\N	1	174954643786848
7	5	3	1	0	超级管理员	1	2025-09-22 16:20:09	超级管理员	1	2025-09-22 16:20:09	\N	1	174954643786848
8	1	4	1	0	超级管理员	1	2025-09-22 16:20:16	超级管理员	1	2025-09-22 16:20:16	\N	1	174954643786848
9	12	5	1	0	超级管理员	1	2025-09-22 16:20:35	超级管理员	1	2025-09-22 16:20:35	\N	1	174954643786848
10	1	15	1	0	超级管理员	1	2025-09-22 16:20:50	超级管理员	1	2025-09-22 16:20:50	\N	1	174954643786848
11	1	25	1	0	超级管理员	1	2025-09-22 16:21:02	超级管理员	1	2025-09-22 16:21:02	\N	1	174954643786848
12	4	25	1	0	超级管理员	1	2025-09-22 16:21:02	超级管理员	1	2025-09-22 16:21:02	\N	1	174954643786848
13	1	24	1	0	超级管理员	1	2025-09-22 16:21:19	超级管理员	1	2025-09-22 16:21:19	\N	1	174954643786848
14	7	24	1	0	超级管理员	1	2025-09-22 16:21:19	超级管理员	1	2025-09-22 16:21:19	\N	1	174954643786848
15	7	23	1	0	超级管理员	1	2025-09-22 16:25:38	超级管理员	1	2025-09-22 16:25:38	\N	1	174954643786848
16	2	23	1	0	超级管理员	1	2025-09-22 16:25:38	超级管理员	1	2025-09-22 16:25:38	\N	1	174954643786848
17	1	22	1	0	超级管理员	1	2025-09-22 16:25:49	超级管理员	1	2025-09-22 16:25:49	\N	1	174954643786848
18	7	22	1	0	超级管理员	1	2025-09-22 16:25:49	超级管理员	1	2025-09-22 16:25:49	\N	1	174954643786848
\.


--
-- Name: ast_asset_column_space_rel ast_asset_column_project_rel_pkey; Type: CONSTRAINT; Schema: public; Owner: datamaster
--

ALTER TABLE ONLY public.ast_asset_column_space_rel
    ADD CONSTRAINT ast_asset_column_project_rel_pkey PRIMARY KEY (id);


--
-- Name: std_desensitize_assetcolumn std_desensitize_assetcolumn_pkey; Type: CONSTRAINT; Schema: public; Owner: datamaster
--

ALTER TABLE ONLY public.std_desensitize_assetcolumn
    ADD CONSTRAINT std_desensitize_assetcolumn_pkey PRIMARY KEY (id);


--
-- Name: idx_ast_asset_column_project_rel_column; Type: INDEX; Schema: public; Owner: datamaster
--

CREATE INDEX idx_ast_asset_column_project_rel_column ON public.ast_asset_column_space_rel USING btree (column_id);


--
-- Name: idx_ast_asset_column_project_rel_project; Type: INDEX; Schema: public; Owner: datamaster
--

CREATE INDEX idx_ast_asset_column_project_rel_project ON public.ast_asset_column_space_rel USING btree (space_id, space_code);


--
-- Name: idx_std_desensitize_assetcolumn_asset; Type: INDEX; Schema: public; Owner: datamaster
--

CREATE INDEX idx_std_desensitize_assetcolumn_asset ON public.std_desensitize_assetcolumn USING btree (asset_id);


--
-- Name: idx_std_desensitize_assetcolumn_column; Type: INDEX; Schema: public; Owner: datamaster
--

CREATE INDEX idx_std_desensitize_assetcolumn_column ON public.std_desensitize_assetcolumn USING btree (assetcolumn_id);


--
-- Name: idx_std_desensitize_assetcolumn_project; Type: INDEX; Schema: public; Owner: datamaster
--

CREATE INDEX idx_std_desensitize_assetcolumn_project ON public.std_desensitize_assetcolumn USING btree (space_id, space_code);


--
-- PostgreSQL database dump complete
--

