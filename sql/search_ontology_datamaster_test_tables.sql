-- =============================================
-- 搜索本体对应的 datamaster_test 测试数据表 (PostgreSQL)
-- 对应搜索本体的三个概念: 公司 / 人物 / 订单
-- 目标库: datamaster_test, 应用账号 datamaster 执行, UTF-8
-- 幂等: 可重复执行 (DROP TABLE IF EXISTS 重建)
-- =============================================
SET client_encoding = 'UTF8';

-- 1. 公司表 (对应概念: 公司 concept 2091103100996616194)
DROP TABLE IF EXISTS dm_person_company;
DROP TABLE IF EXISTS dm_order;
DROP TABLE IF EXISTS dm_person;
DROP TABLE IF EXISTS dm_company;

CREATE TABLE dm_company (
    id                 BIGSERIAL PRIMARY KEY,
    company_code       VARCHAR(50)  NOT NULL,            -- 公司编码
    company_name       VARCHAR(100) NOT NULL,            -- 公司名称
    industry           VARCHAR(50),                      -- 所属行业
    founded_date       DATE,                             -- 成立日期
    address            VARCHAR(200),                     -- 注册地址
    registered_capital NUMERIC(15,2),                    -- 注册资本(万元)
    contact_phone      VARCHAR(20),                      -- 联系电话
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE dm_company IS '公司(搜索本体-公司概念)';
COMMENT ON COLUMN dm_company.company_code IS '公司编码';

-- 2. 人物表 (对应概念: 人物 concept 2091103019119607809)
CREATE TABLE dm_person (
    id          BIGSERIAL PRIMARY KEY,
    person_code VARCHAR(50)  NOT NULL,             -- 人物编码(标识)
    person_name VARCHAR(100) NOT NULL,             -- 姓名
    gender      VARCHAR(10) DEFAULT '未知',        -- 性别
    age         INTEGER,                           -- 年龄
    phone       VARCHAR(20),                       -- 电话
    email       VARCHAR(100),                      -- 邮箱
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE dm_person IS '人物(搜索本体-人物概念)';
COMMENT ON COLUMN dm_person.person_code IS '人物编码(标识)';

-- 3. 订单表 (对应概念: 订单 concept 2091354794326163458)
CREATE TABLE dm_order (
    id           BIGSERIAL PRIMARY KEY,
    order_no     VARCHAR(50)  NOT NULL,             -- 订单编号
    person_id    BIGINT       REFERENCES dm_person(id),   -- 下单客户(对应人物), 关系: 人物-下单->订单
    company_id   BIGINT       REFERENCES dm_company(id),  -- 所属公司(对应公司), 关系: 公司-归属->订单
    product_name VARCHAR(200),                      -- 产品名称
    quantity     INTEGER      DEFAULT 1,            -- 数量
    amount       NUMERIC(15,2),                     -- 金额
    status       VARCHAR(20)  DEFAULT 'pending',    -- 状态: pending/completed/cancelled
    order_date   DATE         DEFAULT CURRENT_DATE, -- 下单日期
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE dm_order IS '订单(搜索本体-订单概念)';

-- 4. 就职关系表 (人物 <-> 公司, 多对多, 对应关系: 就职 relation 2091103443709001729)
CREATE TABLE dm_person_company (
    id          BIGSERIAL PRIMARY KEY,
    person_id   BIGINT NOT NULL REFERENCES dm_person(id),
    company_id  BIGINT NOT NULL REFERENCES dm_company(id),
    position    VARCHAR(50),                        -- 职位
    entry_date  DATE,                               -- 入职日期
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (person_id, company_id)
);
COMMENT ON TABLE dm_person_company IS '就职关系(人物<->公司 多对多)';

-- ============ 测试数据 ============
-- 公司
INSERT INTO dm_company (company_code, company_name, industry, founded_date, address, registered_capital, contact_phone) VALUES
('C001', '华岳科技有限公司',  '信息技术',  '2010-05-18', '上海市浦东新区张江高科技园区', 8000.00, '021-61000001'),
('C002', '蓝海金融服务集团',  '金融服务',  '2005-03-22', '北京市朝阳区国贸CBD',        120000.50, '010-66000002'),
('C003', '云帆数据咨询公司',  '数据服务',  '2016-11-07', '深圳市南山区科技园',         5000.00, '0755-88000003'),
('C004', '星辰智能制造股份',  '智能制造',  '2012-08-30', '杭州市滨江区物联网小镇',     30000.00, '0571-77000004');

-- 人物
INSERT INTO dm_person (person_code, person_name, gender, age, phone, email) VALUES
('P1001', '张伟', '男', 28, '13800000001', 'zhangwei@example.com'),
('P1002', '李娜', '女', 35, '13800000002', 'lina@example.com'),
('P1003', '王强', '男', 42, '13800000003', 'wangqiang@example.com'),
('P1004', '赵敏', '女', 26, '13800000004', 'zhaomin@example.com'),
('P1005', '刘洋', '男', 31, '13800000005', 'liuyang@example.com'),
('P1006', '陈静', '女', 37, '13800000006', 'chenjing@example.com');

-- 就职关系 (人物 <-> 公司)
INSERT INTO dm_person_company (person_id, company_id, position, entry_date) VALUES
(1, 1, '研发工程师',   '2018-07-01'),
(1, 3, '数据顾问',     '2022-03-15'),
(2, 2, '风控经理',     '2015-09-01'),
(3, 1, '技术总监',     '2012-11-20'),
(3, 4, '智能制造专家', '2020-06-10'),
(4, 3, '数据分析师',   '2023-02-01'),
(5, 2, '投资分析师',   '2019-04-15'),
(6, 4, '生产主管',     '2017-08-08');

-- 订单 (归属某公司, 由某人物下单)
INSERT INTO dm_order (order_no, person_id, company_id, product_name, quantity, amount, status, order_date) VALUES
('ORD20260001', 1, 1, '数据平台建设服务',  1, 599000.00, 'completed', '2026-01-15'),
('ORD20260002', 1, 3, '数据治理咨询',      2, 80000.00,  'completed', '2026-02-08'),
('ORD20260003', 2, 2, '智能风控系统',      1, 1200000.00,'pending',   '2026-03-12'),
('ORD20260004', 3, 4, '工业质检设备',      5, 350000.00, 'completed', '2026-03-28'),
('ORD20260005', 4, 3, 'BI可视化工具',      1, 150000.00, 'completed', '2026-04-05'),
('ORD20260006', 5, 2, '金融数据API接口',   3, 45000.00,  'cancelled', '2026-05-01'),
('ORD20260007', 6, 4, '生产MES升级',       1, 780000.00, 'pending',   '2026-06-15'),
('ORD20260008', 2, 1, '大数据集群改造',    1, 960000.00, 'completed', '2026-07-22');

-- ============ 验证 ============
SELECT count(*) AS company_cnt FROM dm_company;
SELECT count(*) AS person_cnt  FROM dm_person;
SELECT count(*) AS relation_cnt FROM dm_person_company;
SELECT count(*) AS order_cnt   FROM dm_order;