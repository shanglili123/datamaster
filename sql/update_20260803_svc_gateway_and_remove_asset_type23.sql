-- 2026-08-03: 资产侧 type2/3(外部API/地理空间服务) 子功能整体下线，保留数据服务侧网关 API/GIS 功能。
-- PostgreSQL migration。
-- 背景：
--   1) 代码侧已删除 datamaster-assets 的 type2/3 全部后端文件与前端入口，
--      资产收敛为纯数据资产(库表/矢量/视频/文件等)。
--   2) 数据服务侧网关转发(transmitType 1=网关API、2=网关GIS)为保留功能，
--      依赖 SVC_API_GATEWAY / SVC_API_GATEWAY_PARAM / SVC_GIS_GATEWAY 三张表。
-- 范围：
--   1) 创建网关三张表(若已存在则跳过)。
--   2) 删除存量 type2/3 资产及其子数据(ast_asset_column / *_space_rel / *_theme_rel / *_apply / ai_skill_ref)。
--   3) DROP 旧子表 ast_asset_api / ast_asset_api_param / ast_asset_gis。
--   4) 清理字典：ast_asset_type 的 2/3、svc_api_bas_info_api_service_type 的 3、
--      以及 type2/3 专用字典 ast_asset_api_column_type / ast_asset_api_method / ast_asset_gis_type。
--   5) 清理 gen 元数据 gen_table / gen_table_column。
--   6) 删除数据服务侧 api_service_type=3 的三方转发服务(svc_api)。

-- ============================================================================
-- Step 1: 创建网关表(数据服务网关功能保留)
-- ============================================================================
CREATE TABLE IF NOT EXISTS svc_api_gateway (
    id             BIGINT        NOT NULL PRIMARY KEY,
    asset_id       BIGINT,
    url            VARCHAR(1000) NOT NULL,
    developer_name VARCHAR(255),
    app_name       VARCHAR(255),
    http_method    VARCHAR(32),
    description    VARCHAR(500),
    valid_flag     BOOLEAN       DEFAULT TRUE,
    del_flag       BOOLEAN       DEFAULT FALSE,
    create_by      VARCHAR(64),
    creator_id     BIGINT,
    create_time    TIMESTAMP,
    update_by      VARCHAR(64),
    updater_id     BIGINT,
    update_time    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS svc_api_gateway_param (
    id             BIGINT        NOT NULL PRIMARY KEY,
    api_id         BIGINT,
    parent_id      BIGINT,
    name           VARCHAR(255)  NOT NULL,
    type           VARCHAR(32)   NOT NULL,
    request_flag   VARCHAR(8)    NOT NULL DEFAULT '0',
    column_type    VARCHAR(32)   NOT NULL,
    valid_flag     BOOLEAN       DEFAULT TRUE,
    del_flag       BOOLEAN       DEFAULT FALSE,
    default_value  VARCHAR(255),
    example_value  VARCHAR(255),
    description    VARCHAR(500),
    create_by      VARCHAR(64),
    creator_id     BIGINT,
    create_time    TIMESTAMP,
    update_by      VARCHAR(64),
    updater_id     BIGINT,
    update_time    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS svc_gis_gateway (
    id                BIGINT        NOT NULL PRIMARY KEY,
    asset_id          BIGINT,
    url               VARCHAR(1000) NOT NULL,
    type              VARCHAR(8),
    http_method       VARCHAR(32),
    coordinate_system VARCHAR(64),
    valid_flag        BOOLEAN       DEFAULT TRUE,
    del_flag          BOOLEAN       DEFAULT FALSE,
    create_by         VARCHAR(64),
    creator_id        BIGINT,
    create_time       TIMESTAMP,
    update_by         VARCHAR(64),
    updater_id        BIGINT,
    update_time       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_svc_api_gateway_asset_id     ON svc_api_gateway (asset_id);
CREATE INDEX IF NOT EXISTS idx_svc_api_gateway_param_api_id ON svc_api_gateway_param (api_id);
CREATE INDEX IF NOT EXISTS idx_svc_gis_gateway_asset_id     ON svc_gis_gateway (asset_id);

COMMENT ON TABLE  svc_api_gateway            IS '数据服务-API网关';
COMMENT ON TABLE  svc_api_gateway_param      IS '数据服务-API网关-参数';
COMMENT ON TABLE  svc_gis_gateway            IS '数据服务-GIS网关';

-- ============================================================================
-- Step 2: 删除存量 type2/3 资产及其子数据
-- ============================================================================

-- 2a. AI skill 引用(先于资产/字段删除)
DELETE FROM ai_skill_ref
WHERE  ref_type = 'TABLE'
   AND ref_id IN (SELECT id FROM ast_asset WHERE type IN ('2', '3'));

DELETE FROM ai_skill_ref
WHERE  ref_type = 'COLUMN'
   AND ref_id IN (SELECT id FROM ast_asset_column
                  WHERE asset_id IN (SELECT id FROM ast_asset WHERE type IN ('2', '3')));

-- accc 测试资产生成的问数 Skill，随资产一并下线
DELETE FROM ai_skill WHERE id = '2076260213601701889';

-- 2b. 资产字段及空间/主题关联
DELETE FROM ast_asset_column_space_rel
WHERE  asset_id IN (SELECT id FROM ast_asset WHERE type IN ('2', '3'));

DELETE FROM ast_asset_column
WHERE  asset_id IN (SELECT id FROM ast_asset WHERE type IN ('2', '3'));

DELETE FROM ast_asset_space_rel
WHERE  asset_id IN (SELECT id FROM ast_asset WHERE type IN ('2', '3'));

DELETE FROM ast_asset_theme_rel
WHERE  asset_id IN (SELECT id FROM ast_asset WHERE type IN ('2', '3'));

DELETE FROM ast_asset_apply
WHERE  asset_id IN (SELECT id FROM ast_asset WHERE type IN ('2', '3'));

-- 2c. 资产本体
DELETE FROM ast_asset WHERE type IN ('2', '3');

-- ============================================================================
-- Step 3: DROP 旧子表
-- ============================================================================
DROP TABLE IF EXISTS ast_asset_gis;
DROP TABLE IF EXISTS ast_asset_api_param;
DROP TABLE IF EXISTS ast_asset_api;

-- ============================================================================
-- Step 4: 清理字典
-- ============================================================================

-- 4a. 资产类型移除 外部API(2)/地理空间服务(3)
DELETE FROM system_dict_data
WHERE  dict_type = 'ast_asset_type' AND dict_value IN ('2', '3');

-- 4b. API服务类型移除 第三方转发(3)
DELETE FROM system_dict_data
WHERE  dict_type = 'svc_api_bas_info_api_service_type' AND dict_value = '3';

-- 4c. type2/3 专用字典类型及数据
DELETE FROM system_dict_data
WHERE  dict_type IN ('ast_asset_api_column_type', 'ast_asset_api_method', 'ast_asset_gis_type');

DELETE FROM system_dict_type
WHERE  dict_type IN ('ast_asset_api_column_type', 'ast_asset_api_method', 'ast_asset_gis_type');

-- ============================================================================
-- Step 5: 清理 gen 元数据
-- ============================================================================
DELETE FROM gen_table_column
WHERE  table_id IN (SELECT table_id FROM gen_table
                    WHERE table_name IN ('AST_ASSET_API', 'AST_ASSET_API_PARAM', 'AST_ASSET_GIS'));

DELETE FROM gen_table
WHERE  table_name IN ('AST_ASSET_API', 'AST_ASSET_API_PARAM', 'AST_ASSET_GIS');

-- ============================================================================
-- Step 6: 删除数据服务侧三方转发服务
-- ============================================================================
DELETE FROM svc_api WHERE api_service_type = '3';
