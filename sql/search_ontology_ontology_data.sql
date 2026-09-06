-- =============================================
-- 完善搜索本体(2091089055300124674): 概念属性 + 概念-表绑定 + 属性-字段映射 + 关系-字段映射
-- 目标库: datamaster (平台主库), 应用账号 datamaster 执行, UTF-8
-- 幂等: 可重复执行 (先软删除旧绑定再插入新绑定)
-- 概念: 公司 2091103100996616194 / 人物 2091103019119607809 / 订单 2091354794326163458
-- 数据源: 2072696163227705346 (datamaster_test), 数据库 datamaster_test, schema public
-- =============================================
SET client_encoding = 'UTF8';

-- ############ 0. 前置: 关系关联表绑定表 (若缺失则创建) ############
CREATE TABLE IF NOT EXISTS ONT_RELATION_TABLE (
    ID              BIGINT       PRIMARY KEY,
    RELATION_ID     BIGINT       NOT NULL,
    DATASOURCE_ID   BIGINT       NOT NULL,
    DATABASE_NAME   VARCHAR(100),
    TABLE_NAME      VARCHAR(100) NOT NULL,
    SCHEMA_NAME     VARCHAR(100),
    COLUMN_NAMES    TEXT,
    CREATOR_ID      BIGINT,
    CREATE_BY       VARCHAR(50),
    CREATE_TIME     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATER_ID      BIGINT,
    UPDATE_BY       VARCHAR(50),
    UPDATE_TIME     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    DEL_FLAG        SMALLINT     DEFAULT 0
);
COMMENT ON TABLE ONT_RELATION_TABLE IS '关系关联表绑定';
COMMENT ON COLUMN ONT_RELATION_TABLE.COLUMN_NAMES IS '已选物理字段名, JSON数组';
CREATE INDEX IF NOT EXISTS IDX_ONT_RTABLE_RID ON ONT_RELATION_TABLE(RELATION_ID);

-- ############ 0. 常量 ############
-- 概念 ID
--   公司 2091103100996616194
--   人物 2091103019119607809
--   订单 2091354794326163458

-- ############ 1. 概念-物理表绑定 (ONT_CONCEPT_TABLE) ############
-- 先软删除旧绑定: 公司->ddd, 人物->user_1 (仅删旧表绑定, 不动 dm_* 新绑定)
UPDATE ont_concept_table SET del_flag=1, update_by='超级管理员', update_time=now()
WHERE del_flag=0
  AND ((concept_id=2091103100996616194 AND table_name='ddd')
    OR (concept_id=2091103019119607809 AND table_name='user_1'));

INSERT INTO ont_concept_table (id, concept_id, datasource_id, database_name, table_name, schema_name, creator_id, create_by, create_time, updater_id, update_by, update_time, del_flag) VALUES
(2095409434279940096, 2091103100996616194, 2072696163227705346, 'datamaster_test', 'dm_company', 'public', 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940097, 2091103019119607809, 2072696163227705346, 'datamaster_test', 'dm_person',  'public', 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940098, 2091354794326163458, 2072696163227705346, 'datamaster_test', 'dm_order',   'public', 1, '超级管理员', now(), 1, '超级管理员', now(), 0)
ON CONFLICT (id) DO NOTHING;

-- ############ 2. 完善概念属性 (ONT_PROPERTY) ############
-- 2.1 修正订单的"金额"属性: data_type string -> decimal
UPDATE ont_property SET data_type='decimal', description='订单金额', update_by='超级管理员', update_time=now()
WHERE id=2091358809713737729 AND del_flag=0;

-- 2.2 新增属性
INSERT INTO ont_property (id, concept_id, name, code, data_type, description, is_primary, is_required, default_value, sort_order, creator_id, create_by, create_time, updater_id, update_by, update_time, del_flag) VALUES
-- 公司 (concept 2091103100996616194)
(2095409434279940100, 2091103100996616194, '公司编码',     'prop_dm_company_code',    'string',  '公司唯一编码',     false, true,  NULL, 1, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940101, 2091103100996616194, '公司名称',     'prop_dm_company_name',    'string',  '公司全称',         false, true,  NULL, 2, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940102, 2091103100996616194, '所属行业',     'prop_dm_industry',        'string',  '公司所属行业',     false, false, NULL, 3, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940103, 2091103100996616194, '成立日期',     'prop_dm_founded_date',    'date',    '公司成立日期',     false, false, NULL, 4, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940104, 2091103100996616194, '注册地址',     'prop_dm_address',         'string',  '公司注册地址',     false, false, NULL, 5, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940105, 2091103100996616194, '注册资本',     'prop_dm_reg_capital',     'decimal', '注册资本(万元)',   false, false, NULL, 6, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
-- 人物 (concept 2091103019119607809)  注: 已有 年龄(2091103263655919618) 与 标识(2093636152165736450)
(2095409434279940106, 2091103019119607809, '姓名',         'prop_dm_person_name',     'string',  '人物姓名',         false, true,  NULL, 1, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940107, 2091103019119607809, '性别',         'prop_dm_gender',          'string',  '性别',             false, false, '未知', 2, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940108, 2091103019119607809, '电话',         'prop_dm_phone',           'string',  '联系电话',         false, false, NULL, 4, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940109, 2091103019119607809, '邮箱',         'prop_dm_email',           'string',  '电子邮箱',         false, false, NULL, 5, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
-- 订单 (concept 2091354794326163458)  注: 金额(2091358809713737729) 已存在(上面已改 decimal)
(2095409434279940110, 2091354794326163458, '订单编号',     'prop_dm_order_no',        'string',  '订单唯一编号',     false, true,  NULL, 1, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940111, 2091354794326163458, '产品名称',     'prop_dm_product_name',    'string',  '订购产品名称',     false, false, NULL, 2, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940112, 2091354794326163458, '数量',         'prop_dm_quantity',        'integer', '订购数量',         false, false, '1',  3, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940113, 2091354794326163458, '状态',         'prop_dm_status',          'string',  '订单状态',         false, false, 'pending', 4, 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940114, 2091354794326163458, '下单日期',     'prop_dm_order_date',      'date',    '订单下单日期',     false, false, NULL, 5, 1, '超级管理员', now(), 1, '超级管理员', now(), 0)
ON CONFLICT (id) DO NOTHING;

-- 2.3 软删除公司概念下无用的旧属性 (测试属性A 2091199256917377026, 年龄 2091331511702716418)
UPDATE ont_property SET del_flag=1, update_by='超级管理员', update_time=now()
WHERE id IN (2091199256917377026, 2091331511702716418) AND del_flag=0;

-- ############ 3. 关系完善 (ONT_RELATION) ############
-- 3.1 软删除冗余关系"拥有"(订单->公司, 与"归属"语义重复): 2091356665879134209
UPDATE ont_relation SET del_flag=1, update_by='超级管理员', update_time=now()
WHERE id=2091356665879134209 AND del_flag=0;

-- 3.2 新增关系"下单" 人物 -> 订单 (one_to_many)
INSERT INTO ont_relation (id, ontology_id, name, code, source_concept_id, target_concept_id, relation_type, description, sort_order, creator_id, create_by, create_time, updater_id, update_by, update_time, del_flag)
VALUES (2095409434279940115, 2091089055300124674, '下单', 'rel_order_by_person', 2091103019119607809, 2091354794326163458, 'one_to_many', '人物产生订单(一对多)', 3, 1, '超级管理员', now(), 1, '超级管理员', now(), 0)
ON CONFLICT (id) DO NOTHING;

-- 3.3 更新"就职"关系描述 (人物<->公司 多对多)
UPDATE ont_relation SET description='人物在公司就职(多对多)', update_by='超级管理员', update_time=now()
WHERE id=2091103443709001729 AND del_flag=0;
-- 更新"归属"关系描述 (公司->订单 一对多)
UPDATE ont_relation SET description='公司归属的订单(一对多)', update_by='超级管理员', update_time=now()
WHERE id=2091358457266372609 AND del_flag=0;

-- ############ 4. 属性-字段映射 (ONT_PROPERTY_COLUMN) ############
-- 先清理旧映射(若有)
DELETE FROM ont_property_column WHERE concept_table_id IN (2095409434279940096, 2095409434279940097, 2095409434279940098);
-- 清理指向已废弃概念表绑定(user_1 id=2093635972729217025)的孤立属性映射
UPDATE ont_property_column SET del_flag=1, update_by='超级管理员', update_time=now()
WHERE concept_table_id=2093635972729217025 AND del_flag=0;

INSERT INTO ont_property_column (id, property_id, concept_table_id, column_name, creator_id, create_by, create_time, updater_id, update_by, update_time, del_flag) VALUES
-- 公司 -> dm_company
(2095409434279940119, 2095409434279940100, 2095409434279940096, 'company_code',        1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940120, 2095409434279940101, 2095409434279940096, 'company_name',        1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940121, 2095409434279940102, 2095409434279940096, 'industry',            1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940122, 2095409434279940103, 2095409434279940096, 'founded_date',        1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940123, 2095409434279940104, 2095409434279940096, 'address',             1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940124, 2095409434279940105, 2095409434279940096, 'registered_capital',  1, '超级管理员', now(), 1, '超级管理员', now(), 0),
-- 人物 -> dm_person
(2095409434279940125, 2095409434279940106, 2095409434279940097, 'person_name',         1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940126, 2095409434279940107, 2095409434279940097, 'gender',              1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940127, 2095409434279940108, 2095409434279940097, 'phone',               1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434279940128, 2095409434279940109, 2095409434279940097, 'email',               1, '超级管理员', now(), 1, '超级管理员', now(), 0),
-- 人物已有属性: 年龄 -> age, 标识 -> person_code
(2095409434279940129, 2091103263655919618, 2095409434279940097, 'age',                 1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434284134400, 2093636152165736450, 2095409434279940097, 'person_code',         1, '超级管理员', now(), 1, '超级管理员', now(), 0),
-- 订单 -> dm_order
(2095409434284134401, 2091358809713737729, 2095409434279940098, 'amount',              1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434284134402, 2095409434279940110, 2095409434279940098, 'order_no',            1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434284134403, 2095409434279940111, 2095409434279940098, 'product_name',        1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434284134404, 2095409434279940112, 2095409434279940098, 'quantity',            1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434284134405, 2095409434279940113, 2095409434279940098, 'status',              1, '超级管理员', now(), 1, '超级管理员', now(), 0),
(2095409434284134406, 2095409434279940114, 2095409434279940098, 'order_date',          1, '超级管理员', now(), 1, '超级管理员', now(), 0);

-- ############ 5. 关系-字段/关联表绑定 (ONT_RELATION_COLUMN / ONT_RELATION_TABLE) ############
-- 5.1 就职(人物<->公司, many_to_many) -> 绑定关联表 dm_person_company
--     清理就职关系下旧的关系-字段映射(指向已废弃的 actor/ddd 测试表)
UPDATE ont_relation_column SET del_flag=1, update_by='超级管理员', update_time=now()
WHERE relation_id=2091103443709001729 AND del_flag=0;
DELETE FROM ont_relation_table WHERE relation_id=2091103443709001729;
INSERT INTO ont_relation_table (id, relation_id, datasource_id, database_name, table_name, schema_name, column_names, creator_id, create_by, create_time, updater_id, update_by, update_time, del_flag)
VALUES (2095409434279940116, 2091103443709001729, 2072696163227705346, 'datamaster_test', 'dm_person_company', 'public', '["person_id","company_id","position","entry_date"]', 1, '超级管理员', now(), 1, '超级管理员', now(), 0);

-- 5.2 归属(公司->订单, one_to_many) -> 字段映射 source=dm_company.id, target=dm_order.company_id
DELETE FROM ont_relation_column WHERE relation_id=2091358457266372609;
INSERT INTO ont_relation_column (id, relation_id, source_concept_table_id, source_column, target_concept_table_id, target_column, creator_id, create_by, create_time, updater_id, update_by, update_time, del_flag)
VALUES (2095409434279940117, 2091358457266372609, 2095409434279940096, 'id', 2095409434279940098, 'company_id', 1, '超级管理员', now(), 1, '超级管理员', now(), 0);

-- 5.3 下单(人物->订单, one_to_many) -> 字段映射 source=dm_person.id, target=dm_order.person_id
INSERT INTO ont_relation_column (id, relation_id, source_concept_table_id, source_column, target_concept_table_id, target_column, creator_id, create_by, create_time, updater_id, update_by, update_time, del_flag)
VALUES (2095409434284134407, 2095409434279940115, 2095409434279940097, 'id', 2095409434279940098, 'person_id', 1, '超级管理员', now(), 1, '超级管理员', now(), 0)
ON CONFLICT (id) DO NOTHING;

-- ############ 6. 验证 ############
SELECT 'concept_table' AS sec, count(*) FROM ont_concept_table WHERE del_flag=0 AND concept_id IN (2091103100996616194,2091103019119607809,2091354794326163458);
SELECT 'property_active' AS sec, count(*) FROM ont_property WHERE del_flag=0 AND concept_id IN (2091103100996616194,2091103019119607809,2091354794326163458);
SELECT 'relation_active' AS sec, count(*) FROM ont_relation WHERE del_flag=0 AND ontology_id=2091089055300124674;
SELECT 'property_column' AS sec, count(*) FROM ont_property_column WHERE del_flag=0;