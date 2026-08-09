-- 2026-07-30: unify 10+ TAX_*_CAT tables into single TAX_CATEGORY table + cat_type discriminator.
-- PostgreSQL migration. Does NOT keep old tables for compatibility.

-- ============================================================================
-- Step 1: create unified category table
-- ============================================================================
CREATE TABLE IF NOT EXISTS tax_category (
    id            BIGINT        NOT NULL PRIMARY KEY,
    cat_type      VARCHAR(32)   NOT NULL,
    name          VARCHAR(255)  NOT NULL,
    parent_id     BIGINT        DEFAULT 0,
    sort_order    BIGINT,
    description   VARCHAR(500),
    code          VARCHAR(255),
    space_id      BIGINT,
    space_code    VARCHAR(64),
    valid_flag    BOOLEAN       DEFAULT TRUE,
    del_flag      BOOLEAN       DEFAULT FALSE,
    create_by     VARCHAR(64),
    creator_id    BIGINT,
    create_time   TIMESTAMP,
    update_by     VARCHAR(64),
    updater_id    BIGINT,
    update_time   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tax_category_cat_type ON tax_category (cat_type);
CREATE INDEX IF NOT EXISTS idx_tax_category_parent   ON tax_category (parent_id);
CREATE INDEX IF NOT EXISTS idx_tax_category_code     ON tax_category (code);
CREATE INDEX IF NOT EXISTS idx_tax_category_space    ON tax_category (space_id);

COMMENT ON TABLE  tax_category              IS '统一分类表';
COMMENT ON COLUMN tax_category.cat_type     IS '类目类型: ASSET/API/TASK/MODEL/DATA_ELEM/DATA_DEV/DOCUMENT/QUALITY/TAG/CLEAN';
COMMENT ON COLUMN tax_category.name         IS '类别名称';
COMMENT ON COLUMN tax_category.parent_id    IS '关联上级ID, 0表示根节点';
COMMENT ON COLUMN tax_category.sort_order   IS '类别排序';
COMMENT ON COLUMN tax_category.description  IS '描述';
COMMENT ON COLUMN tax_category.code         IS '层级编码, 如 A01B02C03';
COMMENT ON COLUMN tax_category.space_id     IS '空间ID';
COMMENT ON COLUMN tax_category.space_code   IS '空间编码';
COMMENT ON COLUMN tax_category.valid_flag   IS '是否有效';

-- ============================================================================
-- Step 2: migrate data from each old table, populating cat_type
-- ============================================================================

-- 2a. TAX_ASSET_CAT → cat_type=ASSET
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'ASSET', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_asset_cat
ON CONFLICT (id) DO NOTHING;

-- 2b. TAX_API_CAT → cat_type=API
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'API', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_api_cat
ON CONFLICT (id) DO NOTHING;

-- 2c. TAX_TASK_CAT → cat_type=TASK
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'TASK', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_task_cat
ON CONFLICT (id) DO NOTHING;

-- 2d. TAX_MODEL_CAT → cat_type=MODEL
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'MODEL', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_model_cat
ON CONFLICT (id) DO NOTHING;

-- 2e. TAX_DATA_ELEM_CAT → cat_type=DATA_ELEM
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'DATA_ELEM', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_data_elem_cat
ON CONFLICT (id) DO NOTHING;

-- 2f. TAX_DATA_DEV_CAT → cat_type=DATA_DEV
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'DATA_DEV', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_data_dev_cat
ON CONFLICT (id) DO NOTHING;

-- 2g. TAX_DOCUMENT_CAT → cat_type=DOCUMENT
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'DOCUMENT', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_document_cat
ON CONFLICT (id) DO NOTHING;

-- 2h. TAX_QUALITY_CAT → cat_type=QUALITY
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'QUALITY', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_quality_cat
ON CONFLICT (id) DO NOTHING;

-- 2i. TAX_TAG_CAT → cat_type=TAG
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'TAG', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_tag_cat
ON CONFLICT (id) DO NOTHING;

-- 2j. TAX_CLEAN_CAT → cat_type=CLEAN
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'CLEAN', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_clean_cat
ON CONFLICT (id) DO NOTHING;

-- 2k. TAX_DISCOVER_TASK_CAT (legacy) → cat_type=TASK
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'TASK', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_discover_task_cat
ON CONFLICT (id) DO NOTHING;

-- 2l. TAX_JOB_CAT (legacy) → cat_type=DATA_DEV
INSERT INTO tax_category (id, cat_type, name, parent_id, sort_order, description, code, space_id, space_code, valid_flag, del_flag, create_by, creator_id, create_time, update_by, updater_id, update_time)
SELECT id, 'DATA_DEV', name, parent_id, sort_order, description, code, space_id, space_code, valid_flag::boolean, del_flag::boolean, create_by, creator_id, create_time, update_by, updater_id, update_time
FROM tax_job_cat
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Step 3: verify migrated data
-- ============================================================================
DO $$
DECLARE
    v_total_old BIGINT;
    v_total_new BIGINT;
BEGIN
    SELECT (
        (SELECT count(*) FROM tax_asset_cat) +
        (SELECT count(*) FROM tax_api_cat) +
        (SELECT count(*) FROM tax_task_cat) +
        (SELECT count(*) FROM tax_model_cat) +
        (SELECT count(*) FROM tax_data_elem_cat) +
        (SELECT count(*) FROM tax_data_dev_cat) +
        (SELECT count(*) FROM tax_document_cat) +
        (SELECT count(*) FROM tax_quality_cat) +
        (SELECT count(*) FROM tax_tag_cat) +
        (SELECT count(*) FROM tax_clean_cat) +
        COALESCE((SELECT count(*) FROM tax_discover_task_cat), 0) +
        COALESCE((SELECT count(*) FROM tax_job_cat), 0)
    ) INTO v_total_old;

    SELECT count(*) INTO v_total_new FROM tax_category;

    RAISE NOTICE 'Old tables total rows: %, TAX_CATEGORY total rows: %', v_total_old, v_total_new;

    IF v_total_old != v_total_new THEN
        RAISE WARNING 'Row count mismatch: old=% new=%', v_total_old, v_total_new;
    ELSE
        RAISE NOTICE 'Migration verified OK: % rows', v_total_new;
    END IF;
END $$;

-- ============================================================================
-- Step 4: drop old tables (no compatibility)
-- ============================================================================
DROP TABLE IF EXISTS tax_asset_cat;
DROP TABLE IF EXISTS tax_api_cat;
DROP TABLE IF EXISTS tax_task_cat;
DROP TABLE IF EXISTS tax_model_cat;
DROP TABLE IF EXISTS tax_data_elem_cat;
DROP TABLE IF EXISTS tax_data_dev_cat;
DROP TABLE IF EXISTS tax_document_cat;
DROP TABLE IF EXISTS tax_quality_cat;
DROP TABLE IF EXISTS tax_tag_cat;
DROP TABLE IF EXISTS tax_clean_cat;
DROP TABLE IF EXISTS tax_discover_task_cat;
DROP TABLE IF EXISTS tax_job_cat;
