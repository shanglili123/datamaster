-- ============================================================
-- PostgreSQL 备份: db=datamaster_business host=192.168.93.174:5432
-- 导出时间: 2026-09-12 21:07:05
-- 生成工具: backup_database.py (psycopg2, 无 pg_dump 依赖)
-- 建议以 superuser 回放: python run_migration.py <本文件> --db datamaster_business
-- ============================================================
SET statement_timeout = 0;
SET client_encoding = 'UTF8';

CREATE TABLE "dm_order" (
    "id" bigint NOT NULL,
    "order_no" character varying(50) NOT NULL,
    "user_id" bigint,
    "product_id" bigint,
    "quantity" integer DEFAULT 1,
    "amount" numeric(15,2),
    "status" character varying(20) DEFAULT 'pending'::character varying,
    "order_date" date DEFAULT CURRENT_DATE,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dm_order_pkey PRIMARY KEY ("id")
);

CREATE SEQUENCE IF NOT EXISTS "dm_order_id_seq";
ALTER SEQUENCE "dm_order_id_seq" OWNED BY "dm_order"."id";
ALTER TABLE "dm_order" ALTER COLUMN "id" SET DEFAULT nextval('dm_order_id_seq'::regclass);
SELECT setval('dm_order_id_seq', 12, TRUE);

COMMENT ON TABLE "dm_order" IS '订单(业务库-订单)';
COMMENT ON COLUMN "dm_order"."user_id" IS '下单用户ID(与 dm_user.id 对齐)';
COMMENT ON COLUMN "dm_order"."product_id" IS '购买商品ID(与 dm_product.id 对齐)';

INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (1, 'ORD20260001', 1, 1, 1, 6999.00, 'completed', '2026-01-10'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (2, 'ORD20260002', 1, 7, 3, 384.00, 'completed', '2026-01-22'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (3, 'ORD20260003', 2, 5, 2, 1198.00, 'pending', '2026-02-03'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (4, 'ORD20260004', 2, 9, 1, 2990.00, 'completed', '2026-02-18'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (5, 'ORD20260005', 3, 3, 2, 2598.00, 'completed', '2026-03-05'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (6, 'ORD20260006', 3, 8, 4, 359.60, 'cancelled', '2026-03-20'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (7, 'ORD20260007', 4, 6, 1, 880.00, 'completed', '2026-04-12'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (8, 'ORD20260008', 4, 10, 2, 898.00, 'pending', '2026-04-28'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (9, 'ORD20260009', 5, 2, 1, 7999.00, 'completed', '2026-05-15'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (10, 'ORD20260010', 5, 4, 5, 1995.00, 'completed', '2026-06-01'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (11, 'ORD20260011', 6, 1, 2, 13998.00, 'pending', '2026-06-25'::date, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_order" ("id", "order_no", "user_id", "product_id", "quantity", "amount", "status", "order_date", "created_at") VALUES (12, 'ORD20260012', 6, 7, 10, 1280.00, 'completed', '2026-07-02'::date, '2026-09-11T09:00:00'::timestamp);

CREATE TABLE "dm_product" (
    "id" bigint NOT NULL,
    "product_code" character varying(50) NOT NULL,
    "product_name" character varying(200) NOT NULL,
    "category" character varying(50),
    "price" numeric(15,2),
    "stock" integer DEFAULT 0,
    "warehouse_id" bigint,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dm_product_pkey PRIMARY KEY ("id")
);

CREATE SEQUENCE IF NOT EXISTS "dm_product_id_seq";
ALTER SEQUENCE "dm_product_id_seq" OWNED BY "dm_product"."id";
ALTER TABLE "dm_product" ALTER COLUMN "id" SET DEFAULT nextval('dm_product_id_seq'::regclass);
SELECT setval('dm_product_id_seq', 10, TRUE);

COMMENT ON TABLE "dm_product" IS '商品(业务库-商品)';
COMMENT ON COLUMN "dm_product"."product_code" IS '商品编码';
COMMENT ON COLUMN "dm_product"."warehouse_id" IS '所在仓库ID(与 dm_warehouse.id 对齐)';

INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (1, 'P0001', '华为Mate60手机', '电子产品', 6999.00, 120, 1, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (2, 'P0002', '联想拯救者笔记本', '电子产品', 7999.00, 80, 1, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (3, 'P0003', '小米空气净化器', '家用电器', 1299.00, 200, 2, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (4, 'P0004', '美的电饭煲', '家用电器', 399.00, 500, 2, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (5, 'P0005', '李宁运动鞋', '服饰鞋帽', 599.00, 300, 3, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (6, 'P0006', '雅诗兰黛面霜', '美妆个护', 880.00, 150, 3, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (7, 'P0007', '三只松鼠坚果礼盒', '食品饮料', 128.00, 1000, 4, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (8, 'P0008', '金龙鱼食用油5L', '食品饮料', 89.90, 800, 4, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (9, 'P0009', '戴森吹风机', '家用电器', 2990.00, 60, 2, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_product" ("id", "product_code", "product_name", "category", "price", "stock", "warehouse_id", "created_at", "updated_at") VALUES (10, 'P0010', '耐克双肩包', '服饰鞋帽', 449.00, 220, 3, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);

CREATE TABLE "dm_user" (
    "id" bigint NOT NULL,
    "user_code" character varying(50) NOT NULL,
    "user_name" character varying(100) NOT NULL,
    "gender" character varying(10) DEFAULT '未知'::character varying,
    "age" integer,
    "phone" character varying(20),
    "email" character varying(100),
    "register_date" date DEFAULT CURRENT_DATE,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dm_user_pkey PRIMARY KEY ("id")
);

CREATE SEQUENCE IF NOT EXISTS "dm_user_id_seq";
ALTER SEQUENCE "dm_user_id_seq" OWNED BY "dm_user"."id";
ALTER TABLE "dm_user" ALTER COLUMN "id" SET DEFAULT nextval('dm_user_id_seq'::regclass);
SELECT setval('dm_user_id_seq', 6, TRUE);

COMMENT ON TABLE "dm_user" IS '用户(业务库-用户)';
COMMENT ON COLUMN "dm_user"."user_code" IS '用户编码';

INSERT INTO "dm_user" ("id", "user_code", "user_name", "gender", "age", "phone", "email", "register_date", "created_at", "updated_at") VALUES (1, 'U1001', '张伟', '男', 28, '13800001001', 'zhangwei@example.com', '2023-06-01'::date, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_user" ("id", "user_code", "user_name", "gender", "age", "phone", "email", "register_date", "created_at", "updated_at") VALUES (2, 'U1002', '李娜', '女', 35, '13800001002', 'lina@example.com', '2023-08-15'::date, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_user" ("id", "user_code", "user_name", "gender", "age", "phone", "email", "register_date", "created_at", "updated_at") VALUES (3, 'U1003', '王强', '男', 42, '13800001003', 'wangqiang@example.com', '2024-02-20'::date, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_user" ("id", "user_code", "user_name", "gender", "age", "phone", "email", "register_date", "created_at", "updated_at") VALUES (4, 'U1004', '赵敏', '女', 26, '13800001004', 'zhaomin@example.com', '2024-05-10'::date, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_user" ("id", "user_code", "user_name", "gender", "age", "phone", "email", "register_date", "created_at", "updated_at") VALUES (5, 'U1005', '刘洋', '男', 31, '13800001005', 'liuyang@example.com', '2025-01-05'::date, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_user" ("id", "user_code", "user_name", "gender", "age", "phone", "email", "register_date", "created_at", "updated_at") VALUES (6, 'U1006', '陈静', '女', 37, '13800001006', 'chenjing@example.com', '2025-03-18'::date, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);

CREATE TABLE "dm_warehouse" (
    "id" bigint NOT NULL,
    "warehouse_code" character varying(50) NOT NULL,
    "warehouse_name" character varying(100) NOT NULL,
    "location" character varying(200),
    "manager" character varying(50),
    "capacity" integer DEFAULT 0,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dm_warehouse_pkey PRIMARY KEY ("id")
);

CREATE SEQUENCE IF NOT EXISTS "dm_warehouse_id_seq";
ALTER SEQUENCE "dm_warehouse_id_seq" OWNED BY "dm_warehouse"."id";
ALTER TABLE "dm_warehouse" ALTER COLUMN "id" SET DEFAULT nextval('dm_warehouse_id_seq'::regclass);
SELECT setval('dm_warehouse_id_seq', 4, TRUE);

COMMENT ON TABLE "dm_warehouse" IS '仓库(业务库-仓库)';
COMMENT ON COLUMN "dm_warehouse"."warehouse_code" IS '仓库编码';

INSERT INTO "dm_warehouse" ("id", "warehouse_code", "warehouse_name", "location", "manager", "capacity", "created_at", "updated_at") VALUES (1, 'WH001', '上海主仓', '上海市浦东新区', '周正国', 100000, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_warehouse" ("id", "warehouse_code", "warehouse_name", "location", "manager", "capacity", "created_at", "updated_at") VALUES (2, 'WH002', '北京分仓', '北京市大兴区', '吴丽华', 80000, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_warehouse" ("id", "warehouse_code", "warehouse_name", "location", "manager", "capacity", "created_at", "updated_at") VALUES (3, 'WH003', '广州分仓', '广州市黄埔区', '郑国栋', 60000, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);
INSERT INTO "dm_warehouse" ("id", "warehouse_code", "warehouse_name", "location", "manager", "capacity", "created_at", "updated_at") VALUES (4, 'WH004', '成都分仓', '成都市青白江区', '冯雅芳', 50000, '2026-09-11T09:00:00'::timestamp, '2026-09-11T09:00:00'::timestamp);

-- 备份完成: 4 表, 0 个函数