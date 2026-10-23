-- ============================================================================
-- 公司研发生产订单演示数据库 datamaster_company: 客户/产品/研发项目/订单/生产工单
-- 表: dm_customer(客户) / dm_product(产品) / dm_rnd_project(研发项目, 关联产品)
--      / dm_order(订单, 关联客户+产品) / dm_production(生产工单, 关联订单+产品)
-- 执行: python run_migration.py update_20260912_dm_company.sql --db datamaster_company
-- 注意: 所有表/序列 owner 统一改为 datamaster (应用连接账号), 避免权限问题
--       数据 seed=42 固定可复现 (客户12 产品18 研发项目15 订单25 生产工单28)
-- ============================================================================

-- 1. 客户
CREATE TABLE dm_customer (
    "id" bigint NOT NULL,
    "customer_no" character varying(20) NOT NULL,
    "name" character varying(100) NOT NULL,
    "industry" character varying(30) NOT NULL,
    "contact" character varying(50) NOT NULL,
    "phone" character varying(20) NOT NULL,
    "level" character varying(10) NOT NULL,
    "status" character varying(10) NOT NULL,
    CONSTRAINT dm_customer_pkey PRIMARY KEY ("id"),
    CONSTRAINT dm_customer_no_uk UNIQUE ("customer_no")
);
CREATE SEQUENCE IF NOT EXISTS "dm_customer_id_seq";
ALTER SEQUENCE "dm_customer_id_seq" OWNED BY "dm_customer"."id";
ALTER TABLE "dm_customer" ALTER COLUMN "id" SET DEFAULT nextval('dm_customer_id_seq'::regclass);
SELECT setval('dm_customer_id_seq', 12, TRUE);
COMMENT ON TABLE "dm_customer" IS '客户(公司库-客户)';
COMMENT ON COLUMN "dm_customer"."customer_no" IS '客户编号';
COMMENT ON COLUMN "dm_customer"."level" IS '客户等级: A/B/C';
COMMENT ON COLUMN "dm_customer"."status" IS '状态: 正常/停用';

INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (1, 'C001', '泰和传动', '汽车配件', '何辰', '13803561597', 'C', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (2, 'C002', '明锐激光', '工程机械', '谢洁', '13856306997', 'A', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (3, 'C003', '远大重工', '医疗器械', '王勇', '13893702683', 'B', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (4, 'C004', '凌云机电', '汽车配件', '赵梅', '13813718431', 'A', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (5, 'C005', '创维软件', '医疗器械', '孙辰', '13835503389', 'A', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (6, 'C006', '宏图电力', '精密仪器', '刘燕', '13810576383', 'C', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (7, 'C007', '瑞丰包装', '工程机械', '高鹏', '13877490893', 'A', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (8, 'C008', '金石智能', '电子制造', '梁霞', '13838840994', 'A', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (9, 'C009', '蓝海智造', '电子制造', '马明', '13860855700', 'C', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (10, 'C010', '恒通精工', '汽车配件', '孙琳', '13828119557', 'C', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (11, 'C011', '鼎诚环保', '工程机械', '郑磊', '13881756179', 'C', '正常');
INSERT INTO "dm_customer" ("id", "customer_no", "name", "industry", "contact", "phone", "level", "status") VALUES (12, 'C012', '北辰阀门', '工程机械', '黄艳', '13862043515', 'B', '正常');

-- 2. 产品
CREATE TABLE dm_product (
    "id" bigint NOT NULL,
    "product_code" character varying(20) NOT NULL,
    "name" character varying(100) NOT NULL,
    "category" character varying(20) NOT NULL,
    "model" character varying(30) NOT NULL,
    "spec" character varying(100) NOT NULL,
    "unit_price" numeric(12,2) NOT NULL,
    "status" character varying(10) NOT NULL,
    CONSTRAINT dm_product_pkey PRIMARY KEY ("id"),
    CONSTRAINT dm_product_code_uk UNIQUE ("product_code")
);
CREATE SEQUENCE IF NOT EXISTS "dm_product_id_seq";
ALTER SEQUENCE "dm_product_id_seq" OWNED BY "dm_product"."id";
ALTER TABLE "dm_product" ALTER COLUMN "id" SET DEFAULT nextval('dm_product_id_seq'::regclass);
SELECT setval('dm_product_id_seq', 18, TRUE);
COMMENT ON TABLE "dm_product" IS '产品(公司库-产品)';
COMMENT ON COLUMN "dm_product"."product_code" IS '产品编号';
COMMENT ON COLUMN "dm_product"."category" IS '产品类别: 传感器/控制器/执行机构/检测仪器/电源模块';
COMMENT ON COLUMN "dm_product"."unit_price" IS '单价(万元)';
COMMENT ON COLUMN "dm_product"."status" IS '状态: 在售/停产/试产';

INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (1, 'P001', '压力传感器-1', '传感器', 'XK-9225', '工作电压24V / 精度±0.1% / IP65', 82.31, '在售');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (2, 'P002', '变频控制器-1', '控制器', 'XK-3852', '工作电压12V / 精度±0.1% / IP65', 96.7, '停产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (3, 'P003', '电动推杆-1', '执行机构', 'XK-4486', '工作电压12V / 精度±0.1% / IP65', 25.71, '试产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (4, 'P004', '三坐标测量机-1', '检测仪器', 'XK-5255', '工作电压24V / 精度±0.1% / IP65', 78.82, '停产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (5, 'P005', '开关电源-1', '电源模块', 'XK-7617', '工作电压24V / 精度±0.1% / IP65', 32.15, '在售');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (6, 'P006', '温度传感器-2', '传感器', 'XK-9297', '工作电压220V / 精度±0.1% / IP65', 89.77, '停产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (7, 'P007', '伺服控制器-2', '控制器', 'XK-9660', '工作电压380V / 精度±0.1% / IP65', 43.76, '在售');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (8, 'P008', '液压缸-2', '执行机构', 'XK-8448', '工作电压380V / 精度±0.1% / IP65', 11.36, '在售');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (9, 'P009', '激光测距仪-2', '检测仪器', 'XK-1896', '工作电压24V / 精度±0.1% / IP65', 75.48, '试产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (10, 'P010', 'UPS电源-2', '电源模块', 'XK-7016', '工作电压12V / 精度±0.1% / IP65', 46.48, '试产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (11, 'P011', '位移传感器-3', '传感器', 'XK-7768', '工作电压220V / 精度±0.1% / IP65', 116.54, '在售');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (12, 'P012', '运动控制器-3', '控制器', 'XK-1976', '工作电压220V / 精度±0.1% / IP65', 92.35, '停产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (13, 'P013', '伺服电机-3', '执行机构', 'XK-1927', '工作电压220V / 精度±0.1% / IP65', 52.45, '停产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (14, 'P014', '红外热像仪-3', '检测仪器', 'XK-0153', '工作电压220V / 精度±0.1% / IP65', 116.64, '在售');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (15, 'P015', '直流电源-3', '电源模块', 'XK-8417', '工作电压12V / 精度±0.1% / IP65', 104.53, '停产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (16, 'P016', '流量传感器-4', '传感器', 'XK-8417', '工作电压24V / 精度±0.1% / IP65', 18.76, '在售');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (17, 'P017', 'PLC控制器-4', '控制器', 'XK-8937', '工作电压12V / 精度±0.1% / IP65', 72.07, '停产');
INSERT INTO "dm_product" ("id", "product_code", "name", "category", "model", "spec", "unit_price", "status") VALUES (18, 'P018', '步进电机-4', '执行机构', 'XK-0419', '工作电压12V / 精度±0.1% / IP65', 111.53, '停产');

-- 3. 研发项目 (关联产品)
CREATE TABLE dm_rnd_project (
    "id" bigint NOT NULL,
    "project_no" character varying(20) NOT NULL,
    "name" character varying(100) NOT NULL,
    "product_code" character varying(20) NOT NULL,
    "manager" character varying(50) NOT NULL,
    "start_date" date NOT NULL,
    "end_date" date NOT NULL,
    "budget" numeric(14,2) NOT NULL,
    "status" character varying(20) NOT NULL,
    CONSTRAINT dm_rnd_project_pkey PRIMARY KEY ("id"),
    CONSTRAINT dm_rnd_project_no_uk UNIQUE ("project_no")
);
CREATE SEQUENCE IF NOT EXISTS "dm_rnd_project_id_seq";
ALTER SEQUENCE "dm_rnd_project_id_seq" OWNED BY "dm_rnd_project"."id";
ALTER TABLE "dm_rnd_project" ALTER COLUMN "id" SET DEFAULT nextval('dm_rnd_project_id_seq'::regclass);
SELECT setval('dm_rnd_project_id_seq', 15, TRUE);
COMMENT ON TABLE "dm_rnd_project" IS '研发项目(公司库-研发项目)';
COMMENT ON COLUMN "dm_rnd_project"."project_no" IS '项目编号';
COMMENT ON COLUMN "dm_rnd_project"."product_code" IS '关联产品编号(与 dm_product.product_code 对齐)';
COMMENT ON COLUMN "dm_rnd_project"."budget" IS '研发预算(万元)';
COMMENT ON COLUMN "dm_rnd_project"."status" IS '状态: 进行中/已结项/已终止';

INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (1, 'R001', '研发项目-电源模块第1期', 'P001', '张磊', '2023-09-06'::date, '2024-03-11'::date, 162.95, '已终止');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (2, 'R002', '研发项目-检测仪器第2期', 'P002', '林艳', '2023-05-16'::date, '2025-06-24'::date, 300.36, '已终止');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (3, 'R003', '研发项目-电源模块第3期', 'P003', '唐晨', '2024-12-19'::date, '2025-07-25'::date, 1930.51, '已终止');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (4, 'R004', '研发项目-执行机构第4期', 'P004', '胡慧', '2024-08-19'::date, '2025-05-04'::date, 828.03, '进行中');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (5, 'R005', '研发项目-传感器第5期', 'P005', '罗东', '2023-04-28'::date, '2025-08-25'::date, 174.85, '进行中');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (6, 'R006', '研发项目-传感器第6期', 'P006', '黄敏', '2023-01-17'::date, '2024-12-04'::date, 188.44, '进行中');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (7, 'R007', '研发项目-检测仪器第7期', 'P007', '赵东', '2025-03-09'::date, '2027-08-08'::date, 514.11, '已终止');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (8, 'R008', '研发项目-检测仪器第8期', 'P008', '朱娟', '2024-02-18'::date, '2025-04-10'::date, 523.84, '已终止');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (9, 'R009', '研发项目-检测仪器第9期', 'P009', '胡晨', '2024-01-28'::date, '2026-11-21'::date, 740.88, '已终止');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (10, 'R010', '研发项目-检测仪器第10期', 'P010', '宋梅', '2025-02-25'::date, '2026-09-12'::date, 241.91, '进行中');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (11, 'R011', '研发项目-控制器第11期', 'P011', '朱勇', '2023-05-23'::date, '2025-04-18'::date, 1095.76, '进行中');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (12, 'R012', '研发项目-电源模块第12期', 'P012', '刘娜', '2023-11-30'::date, '2025-07-07'::date, 914.1, '进行中');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (13, 'R013', '研发项目-控制器第13期', 'P013', '杨浩', '2023-09-06'::date, '2025-05-07'::date, 1856.42, '进行中');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (14, 'R014', '研发项目-控制器第14期', 'P014', '马芳', '2024-07-18'::date, '2025-09-29'::date, 1809.76, '已结项');
INSERT INTO "dm_rnd_project" ("id", "project_no", "name", "product_code", "manager", "start_date", "end_date", "budget", "status") VALUES (15, 'R015', '研发项目-电源模块第15期', 'P015', '梁涵', '2024-09-06'::date, '2025-06-08'::date, 874.86, '进行中');

-- 4. 订单 (关联客户+产品)
CREATE TABLE dm_order (
    "id" bigint NOT NULL,
    "order_no" character varying(20) NOT NULL,
    "customer_no" character varying(20) NOT NULL,
    "product_code" character varying(20) NOT NULL,
    "quantity" integer NOT NULL,
    "unit_price" numeric(12,2) NOT NULL,
    "amount" numeric(14,2) NOT NULL,
    "order_date" date NOT NULL,
    "delivery_date" date NOT NULL,
    "status" character varying(20) NOT NULL,
    CONSTRAINT dm_order_pkey PRIMARY KEY ("id"),
    CONSTRAINT dm_order_no_uk UNIQUE ("order_no")
);
CREATE SEQUENCE IF NOT EXISTS "dm_order_id_seq";
ALTER SEQUENCE "dm_order_id_seq" OWNED BY "dm_order"."id";
ALTER TABLE "dm_order" ALTER COLUMN "id" SET DEFAULT nextval('dm_order_id_seq'::regclass);
SELECT setval('dm_order_id_seq', 25, TRUE);
COMMENT ON TABLE "dm_order" IS '订单(公司库-订单)';
COMMENT ON COLUMN "dm_order"."order_no" IS '订单编号';
COMMENT ON COLUMN "dm_order"."customer_no" IS '客户编号(与 dm_customer.customer_no 对齐)';
COMMENT ON COLUMN "dm_order"."product_code" IS '产品编号(与 dm_product.product_code 对齐)';
COMMENT ON COLUMN "dm_order"."unit_price" IS '成交单价(万元)';
COMMENT ON COLUMN "dm_order"."amount" IS '订单金额(万元)';
COMMENT ON COLUMN "dm_order"."status" IS '状态: 待排产/生产中/已发货/已完成/已取消';

INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (1, 'O0001', 'C005', 'P007', 489, 43.76, 21398.64, '2026-10-05'::date, '2026-03-04'::date, '已发货');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (2, 'O0002', 'C001', 'P002', 4795, 96.7, 463676.5, '2025-09-15'::date, '2026-06-28'::date, '生产中');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (3, 'O0003', 'C001', 'P017', 666, 72.07, 47998.62, '2024-02-05'::date, '2025-09-01'::date, '待排产');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (4, 'O0004', 'C011', 'P008', 3317, 11.36, 37681.12, '2024-10-18'::date, '2024-09-09'::date, '已取消');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (5, 'O0005', 'C010', 'P002', 681, 96.7, 65852.7, '2025-12-03'::date, '2026-08-21'::date, '已取消');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (6, 'O0006', 'C009', 'P011', 2146, 116.54, 250094.84, '2024-12-08'::date, '2024-11-17'::date, '生产中');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (7, 'O0007', 'C005', 'P013', 1082, 52.45, 56750.9, '2026-11-27'::date, '2026-11-04'::date, '已完成');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (8, 'O0008', 'C006', 'P003', 86, 25.71, 2211.06, '2025-11-15'::date, '2026-07-31'::date, '待排产');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (9, 'O0009', 'C002', 'P018', 1756, 111.53, 195846.68, '2026-05-16'::date, '2026-05-16'::date, '已发货');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (10, 'O0010', 'C002', 'P008', 3037, 11.36, 34500.32, '2025-03-22'::date, '2026-03-25'::date, '已取消');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (11, 'O0011', 'C012', 'P010', 4343, 46.48, 201862.64, '2024-12-07'::date, '2025-07-21'::date, '已发货');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (12, 'O0012', 'C011', 'P004', 1110, 78.82, 87490.2, '2025-03-01'::date, '2025-04-20'::date, '已取消');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (13, 'O0013', 'C003', 'P009', 2318, 75.48, 174962.64, '2026-04-18'::date, '2026-12-18'::date, '生产中');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (14, 'O0014', 'C011', 'P009', 4150, 75.48, 313242.0, '2025-05-09'::date, '2025-02-22'::date, '待排产');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (15, 'O0015', 'C011', 'P014', 2276, 116.64, 265472.64, '2024-01-02'::date, '2024-12-07'::date, '生产中');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (16, 'O0016', 'C011', 'P009', 1333, 75.48, 100614.84, '2026-08-15'::date, '2027-07-19'::date, '已完成');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (17, 'O0017', 'C009', 'P001', 926, 82.31, 76219.06, '2024-12-19'::date, '2024-06-01'::date, '已取消');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (18, 'O0018', 'C001', 'P012', 4781, 92.35, 441525.35, '2026-03-17'::date, '2027-03-17'::date, '生产中');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (19, 'O0019', 'C001', 'P010', 2997, 46.48, 139300.56, '2024-07-02'::date, '2024-08-03'::date, '生产中');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (20, 'O0020', 'C011', 'P004', 2907, 78.82, 229129.74, '2026-07-28'::date, '2027-09-28'::date, '生产中');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (21, 'O0021', 'C004', 'P006', 1460, 89.77, 131064.2, '2025-01-13'::date, '2025-07-03'::date, '已发货');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (22, 'O0022', 'C007', 'P008', 2195, 11.36, 24935.2, '2024-12-25'::date, '2024-04-20'::date, '已完成');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (23, 'O0023', 'C001', 'P016', 1832, 18.76, 34368.32, '2024-08-23'::date, '2024-12-24'::date, '已发货');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (24, 'O0024', 'C004', 'P008', 203, 11.36, 2306.08, '2026-04-09'::date, '2027-02-13'::date, '已发货');
INSERT INTO "dm_order" ("id", "order_no", "customer_no", "product_code", "quantity", "unit_price", "amount", "order_date", "delivery_date", "status") VALUES (25, 'O0025', 'C005', 'P003', 2296, 25.71, 59030.16, '2025-11-25'::date, '2026-06-06'::date, '已完成');

-- 5. 生产工单 (关联订单+产品)
CREATE TABLE dm_production (
    "id" bigint NOT NULL,
    "work_no" character varying(20) NOT NULL,
    "order_no" character varying(20) NOT NULL,
    "product_code" character varying(20) NOT NULL,
    "quantity" integer NOT NULL,
    "start_date" date NOT NULL,
    "end_date" date NOT NULL,
    "workshop" character varying(30) NOT NULL,
    "status" character varying(20) NOT NULL,
    CONSTRAINT dm_production_pkey PRIMARY KEY ("id"),
    CONSTRAINT dm_production_no_uk UNIQUE ("work_no")
);
CREATE SEQUENCE IF NOT EXISTS "dm_production_id_seq";
ALTER SEQUENCE "dm_production_id_seq" OWNED BY "dm_production"."id";
ALTER TABLE "dm_production" ALTER COLUMN "id" SET DEFAULT nextval('dm_production_id_seq'::regclass);
SELECT setval('dm_production_id_seq', 28, TRUE);
COMMENT ON TABLE "dm_production" IS '生产工单(公司库-生产工单)';
COMMENT ON COLUMN "dm_production"."work_no" IS '工单编号';
COMMENT ON COLUMN "dm_production"."order_no" IS '订单编号(与 dm_order.order_no 对齐)';
COMMENT ON COLUMN "dm_production"."product_code" IS '产品编号(与 dm_product.product_code 对齐)';
COMMENT ON COLUMN "dm_production"."workshop" IS '生产车间';
COMMENT ON COLUMN "dm_production"."status" IS '状态: 待排产/生产中/已完工/已暂停';

INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (1, 'W0001', 'O0022', 'P008', 2174, '2025-07-03'::date, '2025-11-09'::date, '一号车间', '待排产');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (2, 'W0002', 'O0009', 'P018', 1034, '2026-09-29'::date, '2026-03-20'::date, '一号车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (3, 'W0003', 'O0012', 'P004', 959, '2025-11-18'::date, '2027-06-13'::date, '测试车间', '待排产');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (4, 'W0004', 'O0013', 'P009', 2201, '2026-07-14'::date, '2027-06-06'::date, '一号车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (5, 'W0005', 'O0001', 'P007', 371, '2027-07-06'::date, '2027-02-08'::date, '三号车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (6, 'W0006', 'O0003', 'P017', 648, '2024-12-04'::date, '2025-10-04'::date, '一号车间', '已完工');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (7, 'W0007', 'O0017', 'P001', 606, '2025-02-22'::date, '2025-10-30'::date, '装配车间', '已完工');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (8, 'W0008', 'O0018', 'P012', 2694, '2027-03-07'::date, '2028-02-16'::date, '二号车间', '已完工');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (9, 'W0009', 'O0013', 'P009', 1794, '2026-01-01'::date, '2027-09-15'::date, '三号车间', '生产中');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (10, 'W0010', 'O0014', 'P009', 3705, '2026-09-14'::date, '2026-10-22'::date, '装配车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (11, 'W0011', 'O0015', 'P014', 1906, '2025-06-07'::date, '2026-08-27'::date, '二号车间', '待排产');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (12, 'W0012', 'O0010', 'P008', 2301, '2026-10-11'::date, '2026-11-18'::date, '一号车间', '生产中');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (13, 'W0013', 'O0022', 'P008', 1438, '2024-07-22'::date, '2024-10-28'::date, '一号车间', '待排产');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (14, 'W0014', 'O0008', 'P003', 85, '2026-09-18'::date, '2025-05-30'::date, '装配车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (15, 'W0015', 'O0021', 'P006', 1150, '2026-12-15'::date, '2027-02-26'::date, '装配车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (16, 'W0016', 'O0008', 'P003', 49, '2026-12-06'::date, '2025-01-12'::date, '一号车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (17, 'W0017', 'O0008', 'P003', 50, '2026-12-14'::date, '2027-11-27'::date, '装配车间', '待排产');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (18, 'W0018', 'O0018', 'P012', 2986, '2026-05-05'::date, '2028-07-23'::date, '二号车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (19, 'W0019', 'O0022', 'P008', 1680, '2025-07-26'::date, '2025-10-11'::date, '装配车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (20, 'W0020', 'O0018', 'P012', 3456, '2026-06-12'::date, '2028-08-30'::date, '装配车间', '已完工');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (21, 'W0021', 'O0025', 'P003', 1431, '2026-10-15'::date, '2026-07-22'::date, '测试车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (22, 'W0022', 'O0021', 'P006', 904, '2026-03-27'::date, '2025-06-08'::date, '三号车间', '生产中');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (23, 'W0023', 'O0009', 'P018', 1172, '2027-07-08'::date, '2026-06-15'::date, '二号车间', '生产中');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (24, 'W0024', 'O0008', 'P003', 59, '2025-06-06'::date, '2026-03-15'::date, '一号车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (25, 'W0025', 'O0014', 'P009', 2761, '2026-04-23'::date, '2027-05-02'::date, '一号车间', '生产中');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (26, 'W0026', 'O0014', 'P009', 2883, '2026-08-22'::date, '2025-02-10'::date, '测试车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (27, 'W0027', 'O0016', 'P009', 670, '2026-12-27'::date, '2027-09-04'::date, '装配车间', '已暂停');
INSERT INTO "dm_production" ("id", "work_no", "order_no", "product_code", "quantity", "start_date", "end_date", "workshop", "status") VALUES (28, 'W0028', 'O0018', 'P012', 4177, '2027-07-14'::date, '2027-03-28'::date, '装配车间', '生产中');

-- 6. 统一设置 owner = datamaster (应用连接账号), 保证 app 可读写
ALTER TABLE "dm_customer" OWNER TO datamaster;
ALTER TABLE "dm_product" OWNER TO datamaster;
ALTER TABLE "dm_rnd_project" OWNER TO datamaster;
ALTER TABLE "dm_order" OWNER TO datamaster;
ALTER TABLE "dm_production" OWNER TO datamaster;
ALTER SEQUENCE "dm_customer_id_seq" OWNER TO datamaster;
ALTER SEQUENCE "dm_product_id_seq" OWNER TO datamaster;
ALTER SEQUENCE "dm_rnd_project_id_seq" OWNER TO datamaster;
ALTER SEQUENCE "dm_order_id_seq" OWNER TO datamaster;
ALTER SEQUENCE "dm_production_id_seq" OWNER TO datamaster;
