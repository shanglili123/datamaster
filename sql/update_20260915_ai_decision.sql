-- ===================================================================================
-- 工厂生产排产 AI 决策模拟库 datamaster_ai_decision: 依据 ai-decision-simulation-data-spec.md
-- 10 类表: customer/product/material/product_material/sales_order/sales_order_item
--          /material_inventory/production_line/production_capacity_slot/business_event
-- 执行: python run_migration.py update_20260915_ai_decision.sql --db datamaster_ai_decision
-- 空间: AI-DECISION-SIM   来源系统: MOCK-ERP   所有者: datamaster   时区: Asia/Shanghai
-- 覆盖场景: 正常x2 / 高优先级紧急 / 物料不足 / 产能不足 / 多订单竞争+重复事件
-- ===================================================================================

-- 1. 客户
CREATE TABLE customer (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "customer_code" character varying(64) NOT NULL,
    "customer_name" character varying(128) NOT NULL,
    "customer_level" character varying(16) NOT NULL,
    "delivery_priority" integer NOT NULL,
    "default_delivery_days" integer NOT NULL,
    "status" character varying(16) NOT NULL,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT customer_pkey PRIMARY KEY ("id"),
    CONSTRAINT customer_code_uk UNIQUE ("customer_code")
);
COMMENT ON TABLE "customer" IS '客户(AI决策模拟库-客户)';
COMMENT ON COLUMN "customer"."customer_code" IS '客户编码';
COMMENT ON COLUMN "customer"."customer_level" IS '客户等级: A/B/C';
COMMENT ON COLUMN "customer"."delivery_priority" IS '交期优先级: 1-100, 越大越优先';
COMMENT ON COLUMN "customer"."status" IS '状态: ACTIVE/INACTIVE';

INSERT INTO "customer" ("id", "space_code", "customer_code", "customer_name", "customer_level", "delivery_priority", "default_delivery_days", "status", "created_at", "updated_at") VALUES ('CUST-0001', 'AI-DECISION-SIM', 'CUST-0001', '华光电子集团', 'A', 100, 7, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "customer" ("id", "space_code", "customer_code", "customer_name", "customer_level", "delivery_priority", "default_delivery_days", "status", "created_at", "updated_at") VALUES ('CUST-0002', 'AI-DECISION-SIM', 'CUST-0002', '星辉重工股份', 'B', 70, 10, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "customer" ("id", "space_code", "customer_code", "customer_name", "customer_level", "delivery_priority", "default_delivery_days", "status", "created_at", "updated_at") VALUES ('CUST-0003', 'AI-DECISION-SIM', 'CUST-0003', '天马机械制造', 'B', 60, 14, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "customer" ("id", "space_code", "customer_code", "customer_name", "customer_level", "delivery_priority", "default_delivery_days", "status", "created_at", "updated_at") VALUES ('CUST-0004', 'AI-DECISION-SIM', 'CUST-0004', '远景精密设备', 'C', 50, 21, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "customer" ("id", "space_code", "customer_code", "customer_name", "customer_level", "delivery_priority", "default_delivery_days", "status", "created_at", "updated_at") VALUES ('CUST-0005', 'AI-DECISION-SIM', 'CUST-0005', '皓宇工程装备', 'C', 30, 30, 'INACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 2. 产品
CREATE TABLE product (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "product_code" character varying(64) NOT NULL,
    "product_name" character varying(128) NOT NULL,
    "product_category" character varying(64) NOT NULL,
    "unit" character varying(16) NOT NULL,
    "standard_minutes" numeric(12,2) NOT NULL,
    "min_batch_qty" integer NOT NULL,
    "active_flag" boolean NOT NULL,
    "status" character varying(16) NOT NULL,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT product_pkey PRIMARY KEY ("id"),
    CONSTRAINT product_code_uk UNIQUE ("product_code")
);
COMMENT ON TABLE "product" IS '产品(AI决策模拟库-产品)';
COMMENT ON COLUMN "product"."product_code" IS '产品编码';
COMMENT ON COLUMN "product"."standard_minutes" IS '每件标准生产分钟数';
COMMENT ON COLUMN "product"."min_batch_qty" IS '最小生产批量';
COMMENT ON COLUMN "product"."active_flag" IS '是否可生产';

INSERT INTO "product" ("id", "space_code", "product_code", "product_name", "product_category", "unit", "standard_minutes", "min_batch_qty", "active_flag", "status", "created_at", "updated_at") VALUES ('PROD-0001', 'AI-DECISION-SIM', 'PROD-0001', '传动轴组件', '机械部件', '件', 10, 10, TRUE, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product" ("id", "space_code", "product_code", "product_name", "product_category", "unit", "standard_minutes", "min_batch_qty", "active_flag", "status", "created_at", "updated_at") VALUES ('PROD-0002', 'AI-DECISION-SIM', 'PROD-0002', '伺服电机总成', '电机部件', '台', 20, 5, TRUE, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product" ("id", "space_code", "product_code", "product_name", "product_category", "unit", "standard_minutes", "min_batch_qty", "active_flag", "status", "created_at", "updated_at") VALUES ('PROD-0003', 'AI-DECISION-SIM', 'PROD-0003', '液压阀体', '液压部件', '件', 35, 5, TRUE, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product" ("id", "space_code", "product_code", "product_name", "product_category", "unit", "standard_minutes", "min_batch_qty", "active_flag", "status", "created_at", "updated_at") VALUES ('PROD-0004', 'AI-DECISION-SIM', 'PROD-0004', '精密减速器', '精密部件', '台', 60, 1, TRUE, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 3. 物料
CREATE TABLE material (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "material_code" character varying(64) NOT NULL,
    "material_name" character varying(128) NOT NULL,
    "unit" character varying(16) NOT NULL,
    "safety_stock" numeric(12,2) NOT NULL,
    "lead_time_days" integer NOT NULL,
    "status" character varying(16) NOT NULL,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT material_pkey PRIMARY KEY ("id"),
    CONSTRAINT material_code_uk UNIQUE ("material_code")
);
COMMENT ON TABLE "material" IS '物料(AI决策模拟库-物料)';
COMMENT ON COLUMN "material"."material_code" IS '物料编码';
COMMENT ON COLUMN "material"."safety_stock" IS '安全库存';
COMMENT ON COLUMN "material"."lead_time_days" IS '采购提前期(天)';
COMMENT ON COLUMN "material"."status" IS '状态: ACTIVE/INACTIVE';

INSERT INTO "material" ("id", "space_code", "material_code", "material_name", "unit", "safety_stock", "lead_time_days", "status", "created_at", "updated_at") VALUES ('MAT-0001', 'AI-DECISION-SIM', 'MAT-0001', '45#钢棒料', 'kg', 500, 7, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "material" ("id", "space_code", "material_code", "material_name", "unit", "safety_stock", "lead_time_days", "status", "created_at", "updated_at") VALUES ('MAT-0002', 'AI-DECISION-SIM', 'MAT-0002', '高碳轴承钢球', '件', 1000, 5, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "material" ("id", "space_code", "material_code", "material_name", "unit", "safety_stock", "lead_time_days", "status", "created_at", "updated_at") VALUES ('MAT-0003', 'AI-DECISION-SIM', 'MAT-0003', '漆包铜线', '米', 800, 3, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "material" ("id", "space_code", "material_code", "material_name", "unit", "safety_stock", "lead_time_days", "status", "created_at", "updated_at") VALUES ('MAT-0004', 'AI-DECISION-SIM', 'MAT-0004', '铝合金锭', 'kg', 200, 10, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "material" ("id", "space_code", "material_code", "material_name", "unit", "safety_stock", "lead_time_days", "status", "created_at", "updated_at") VALUES ('MAT-0005', 'AI-DECISION-SIM', 'MAT-0005', '氟橡胶密封圈', '件', 2000, 4, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 4. 产品物料清单(BOM)
CREATE TABLE product_material (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "product_id" character varying(64) NOT NULL,
    "material_id" character varying(64) NOT NULL,
    "quantity_per_unit" numeric(12,4) NOT NULL,
    "scrap_rate" numeric(8,4) NOT NULL,
    "status" character varying(16) NOT NULL,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT product_material_pkey PRIMARY KEY ("id"),
    CONSTRAINT product_material_uk UNIQUE ("product_id", "material_id")
);
COMMENT ON TABLE "product_material" IS '产品物料清单(AI决策模拟库-BOM)';
COMMENT ON COLUMN "product_material"."product_id" IS '产品ID(与 product.id 对齐)';
COMMENT ON COLUMN "product_material"."material_id" IS '物料ID(与 material.id 对齐)';
COMMENT ON COLUMN "product_material"."quantity_per_unit" IS '生产1个产品所需物料数量';
COMMENT ON COLUMN "product_material"."scrap_rate" IS '损耗率: 0-0.2';

INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0001', 'AI-DECISION-SIM', 'PROD-0001', 'MAT-0001', 1.2, 0.05, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0002', 'AI-DECISION-SIM', 'PROD-0001', 'MAT-0002', 2.0, 0.02, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0003', 'AI-DECISION-SIM', 'PROD-0001', 'MAT-0005', 4.0, 0.01, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0004', 'AI-DECISION-SIM', 'PROD-0002', 'MAT-0001', 0.8, 0.05, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0005', 'AI-DECISION-SIM', 'PROD-0002', 'MAT-0003', 3.0, 0.03, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0006', 'AI-DECISION-SIM', 'PROD-0002', 'MAT-0005', 2.0, 0.02, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0007', 'AI-DECISION-SIM', 'PROD-0003', 'MAT-0004', 1.5, 0.08, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0008', 'AI-DECISION-SIM', 'PROD-0003', 'MAT-0005', 6.0, 0.01, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0009', 'AI-DECISION-SIM', 'PROD-0004', 'MAT-0004', 2.5, 0.1, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0010', 'AI-DECISION-SIM', 'PROD-0004', 'MAT-0002', 8.0, 0.02, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "product_material" ("id", "space_code", "product_id", "material_id", "quantity_per_unit", "scrap_rate", "status", "created_at", "updated_at") VALUES ('PMA-0011', 'AI-DECISION-SIM', 'PROD-0004', 'MAT-0003', 1.5, 0.03, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 5. 销售订单
CREATE TABLE sales_order (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "order_no" character varying(64) NOT NULL,
    "customer_id" character varying(64) NOT NULL,
    "order_status" character varying(32) NOT NULL,
    "order_priority" integer NOT NULL,
    "order_time" timestamp NOT NULL,
    "requested_delivery_time" timestamp NOT NULL,
    "source_system" character varying(64) NOT NULL,
    "remark" character varying(500),
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT sales_order_pkey PRIMARY KEY ("id"),
    CONSTRAINT sales_order_no_uk UNIQUE ("order_no")
);
COMMENT ON TABLE "sales_order" IS '销售订单(AI决策模拟库-订单)';
COMMENT ON COLUMN "sales_order"."order_no" IS '订单编号';
COMMENT ON COLUMN "sales_order"."customer_id" IS '客户ID(与 customer.id 对齐)';
COMMENT ON COLUMN "sales_order"."order_status" IS '状态: CREATED/CONFIRMED/CANCELLED/COMPLETED';
COMMENT ON COLUMN "sales_order"."order_priority" IS '订单优先级: 1-100';
COMMENT ON COLUMN "sales_order"."requested_delivery_time" IS '要求交付时间';
COMMENT ON COLUMN "sales_order"."source_system" IS '来源系统: MOCK-ERP';
COMMENT ON COLUMN "sales_order"."remark" IS '备注(标注测试场景)';

INSERT INTO "sales_order" ("id", "space_code", "order_no", "customer_id", "order_status", "order_priority", "order_time", "requested_delivery_time", "source_system", "remark", "created_at", "updated_at") VALUES ('ORD-20260915-0001', 'AI-DECISION-SIM', 'ORD-20260915-0001', 'CUST-0002', 'CREATED', 70, '2026-09-15 08:30:00', '2026-09-25 09:00:00', 'MOCK-ERP', '正常排产订单1: 库存/产能/交期均充足', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order" ("id", "space_code", "order_no", "customer_id", "order_status", "order_priority", "order_time", "requested_delivery_time", "source_system", "remark", "created_at", "updated_at") VALUES ('ORD-20260915-0002', 'AI-DECISION-SIM', 'ORD-20260915-0002', 'CUST-0003', 'CREATED', 60, '2026-09-15 08:40:00', '2026-09-28 09:00:00', 'MOCK-ERP', '正常排产订单2', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order" ("id", "space_code", "order_no", "customer_id", "order_status", "order_priority", "order_time", "requested_delivery_time", "source_system", "remark", "created_at", "updated_at") VALUES ('ORD-20260915-0003', 'AI-DECISION-SIM', 'ORD-20260915-0003', 'CUST-0001', 'CREATED', 100, '2026-09-15 08:50:00', '2026-09-16 17:00:00', 'MOCK-ERP', '高优先级紧急订单: A级客户, 交期紧, 优先高效率产线', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order" ("id", "space_code", "order_no", "customer_id", "order_status", "order_priority", "order_time", "requested_delivery_time", "source_system", "remark", "created_at", "updated_at") VALUES ('ORD-20260915-0004', 'AI-DECISION-SIM', 'ORD-20260915-0004', 'CUST-0004', 'CREATED', 50, '2026-09-15 08:55:00', '2026-09-22 09:00:00', 'MOCK-ERP', '物料不足订单: 铝合金锭(MAT-0004)净库存小于需求', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order" ("id", "space_code", "order_no", "customer_id", "order_status", "order_priority", "order_time", "requested_delivery_time", "source_system", "remark", "created_at", "updated_at") VALUES ('ORD-20260915-0005', 'AI-DECISION-SIM', 'ORD-20260915-0005', 'CUST-0005', 'CREATED', 30, '2026-09-15 08:57:00', '2026-09-17 09:00:00', 'MOCK-ERP', '产能不足订单: 大数量超出未来3天剩余产能', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order" ("id", "space_code", "order_no", "customer_id", "order_status", "order_priority", "order_time", "requested_delivery_time", "source_system", "remark", "created_at", "updated_at") VALUES ('ORD-20260915-0006', 'AI-DECISION-SIM', 'ORD-20260915-0006', 'CUST-0002', 'CREATED', 70, '2026-09-15 08:59:00', '2026-09-23 09:00:00', 'MOCK-ERP', '多订单竞争/重复事件订单: 与0005争用同一时段, 事件发送两次', '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 6. 订单明细
CREATE TABLE sales_order_item (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "order_id" character varying(64) NOT NULL,
    "product_id" character varying(64) NOT NULL,
    "quantity" integer NOT NULL,
    "delivered_quantity" integer NOT NULL,
    "unit_price" numeric(12,2),
    "status" character varying(16) NOT NULL,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT sales_order_item_pkey PRIMARY KEY ("id")
);
COMMENT ON TABLE "sales_order_item" IS '订单明细(AI决策模拟库-订单明细)';
COMMENT ON COLUMN "sales_order_item"."order_id" IS '订单ID(与 sales_order.id 对齐)';
COMMENT ON COLUMN "sales_order_item"."product_id" IS '产品ID(与 product.id 对齐)';
COMMENT ON COLUMN "sales_order_item"."quantity" IS '订购数量';
COMMENT ON COLUMN "sales_order_item"."delivered_quantity" IS '已交付数量(默认0)';
COMMENT ON COLUMN "sales_order_item"."unit_price" IS '单价(万元)';

INSERT INTO "sales_order_item" ("id", "space_code", "order_id", "product_id", "quantity", "delivered_quantity", "unit_price", "status", "created_at", "updated_at") VALUES ('ITEM-20260915-0001', 'AI-DECISION-SIM', 'ORD-20260915-0001', 'PROD-0001', 20, 0, 0.5, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order_item" ("id", "space_code", "order_id", "product_id", "quantity", "delivered_quantity", "unit_price", "status", "created_at", "updated_at") VALUES ('ITEM-20260915-0002', 'AI-DECISION-SIM', 'ORD-20260915-0001', 'PROD-0002', 10, 0, 1.2, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order_item" ("id", "space_code", "order_id", "product_id", "quantity", "delivered_quantity", "unit_price", "status", "created_at", "updated_at") VALUES ('ITEM-20260915-0003', 'AI-DECISION-SIM', 'ORD-20260915-0002', 'PROD-0003', 10, 0, 2.0, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order_item" ("id", "space_code", "order_id", "product_id", "quantity", "delivered_quantity", "unit_price", "status", "created_at", "updated_at") VALUES ('ITEM-20260915-0004', 'AI-DECISION-SIM', 'ORD-20260915-0002', 'PROD-0004', 2, 0, 5.0, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order_item" ("id", "space_code", "order_id", "product_id", "quantity", "delivered_quantity", "unit_price", "status", "created_at", "updated_at") VALUES ('ITEM-20260915-0005', 'AI-DECISION-SIM', 'ORD-20260915-0003', 'PROD-0004', 5, 0, 5.0, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order_item" ("id", "space_code", "order_id", "product_id", "quantity", "delivered_quantity", "unit_price", "status", "created_at", "updated_at") VALUES ('ITEM-20260915-0006', 'AI-DECISION-SIM', 'ORD-20260915-0004', 'PROD-0003', 50, 0, 2.0, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order_item" ("id", "space_code", "order_id", "product_id", "quantity", "delivered_quantity", "unit_price", "status", "created_at", "updated_at") VALUES ('ITEM-20260915-0007', 'AI-DECISION-SIM', 'ORD-20260915-0004', 'PROD-0004', 3, 0, 5.0, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order_item" ("id", "space_code", "order_id", "product_id", "quantity", "delivered_quantity", "unit_price", "status", "created_at", "updated_at") VALUES ('ITEM-20260915-0008', 'AI-DECISION-SIM', 'ORD-20260915-0005', 'PROD-0002', 120, 0, 1.2, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "sales_order_item" ("id", "space_code", "order_id", "product_id", "quantity", "delivered_quantity", "unit_price", "status", "created_at", "updated_at") VALUES ('ITEM-20260915-0009', 'AI-DECISION-SIM', 'ORD-20260915-0006', 'PROD-0001', 15, 0, 0.5, 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 7. 物料库存
CREATE TABLE material_inventory (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "material_id" character varying(64) NOT NULL,
    "warehouse_code" character varying(64) NOT NULL,
    "available_quantity" numeric(12,2) NOT NULL,
    "reserved_quantity" numeric(12,2) NOT NULL,
    "inbound_quantity" numeric(12,2) NOT NULL,
    "inventory_time" timestamp NOT NULL,
    "status" character varying(16) NOT NULL,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT material_inventory_pkey PRIMARY KEY ("id")
);
COMMENT ON TABLE "material_inventory" IS '物料库存(AI决策模拟库-库存)';
COMMENT ON COLUMN "material_inventory"."material_id" IS '物料ID(与 material.id 对齐)';
COMMENT ON COLUMN "material_inventory"."warehouse_code" IS '仓库编码';
COMMENT ON COLUMN "material_inventory"."available_quantity" IS '可用库存';
COMMENT ON COLUMN "material_inventory"."reserved_quantity" IS '已预留库存';
COMMENT ON COLUMN "material_inventory"."inbound_quantity" IS '在途库存';
COMMENT ON COLUMN "material_inventory"."inventory_time" IS '库存快照时间';

INSERT INTO "material_inventory" ("id", "space_code", "material_id", "warehouse_code", "available_quantity", "reserved_quantity", "inbound_quantity", "inventory_time", "status", "created_at", "updated_at") VALUES ('INV-0001', 'AI-DECISION-SIM', 'MAT-0001', 'WH-MAIN', 20000, 3000, 0, '2026-09-15 08:00:00', 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "material_inventory" ("id", "space_code", "material_id", "warehouse_code", "available_quantity", "reserved_quantity", "inbound_quantity", "inventory_time", "status", "created_at", "updated_at") VALUES ('INV-0002', 'AI-DECISION-SIM', 'MAT-0002', 'WH-MAIN', 50000, 5000, 0, '2026-09-15 08:00:00', 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "material_inventory" ("id", "space_code", "material_id", "warehouse_code", "available_quantity", "reserved_quantity", "inbound_quantity", "inventory_time", "status", "created_at", "updated_at") VALUES ('INV-0003', 'AI-DECISION-SIM', 'MAT-0003', 'WH-MAIN', 30000, 2000, 0, '2026-09-15 08:00:00', 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "material_inventory" ("id", "space_code", "material_id", "warehouse_code", "available_quantity", "reserved_quantity", "inbound_quantity", "inventory_time", "status", "created_at", "updated_at") VALUES ('INV-0004', 'AI-DECISION-SIM', 'MAT-0004', 'WH-MAIN', 50, 0, 0, '2026-09-15 08:00:00', 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "material_inventory" ("id", "space_code", "material_id", "warehouse_code", "available_quantity", "reserved_quantity", "inbound_quantity", "inventory_time", "status", "created_at", "updated_at") VALUES ('INV-0005', 'AI-DECISION-SIM', 'MAT-0005', 'WH-MAIN', 80000, 10000, 0, '2026-09-15 08:00:00', 'ACTIVE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 8. 生产线
CREATE TABLE production_line (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "line_code" character varying(64) NOT NULL,
    "line_name" character varying(128) NOT NULL,
    "supported_categories" character varying(500) NOT NULL,
    "efficiency_rate" numeric(8,4) NOT NULL,
    "status" character varying(16) NOT NULL,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT production_line_pkey PRIMARY KEY ("id"),
    CONSTRAINT production_line_code_uk UNIQUE ("line_code")
);
COMMENT ON TABLE "production_line" IS '生产线(AI决策模拟库-产线)';
COMMENT ON COLUMN "production_line"."line_code" IS '产线编码';
COMMENT ON COLUMN "production_line"."line_name" IS '产线名称';
COMMENT ON COLUMN "production_line"."supported_categories" IS '支持的产品类别(逗号分隔)';
COMMENT ON COLUMN "production_line"."efficiency_rate" IS '产线效率: 0-1';
COMMENT ON COLUMN "production_line"."status" IS '状态: AVAILABLE/MAINTENANCE/DISABLED';

INSERT INTO "production_line" ("id", "space_code", "line_code", "line_name", "supported_categories", "efficiency_rate", "status", "created_at", "updated_at") VALUES ('LINE-0001', 'AI-DECISION-SIM', 'LINE-0001', '一号智能装配线', '机械部件,精密部件', 0.95, 'AVAILABLE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_line" ("id", "space_code", "line_code", "line_name", "supported_categories", "efficiency_rate", "status", "created_at", "updated_at") VALUES ('LINE-0002', 'AI-DECISION-SIM', 'LINE-0002', '二号标准产线', '电机部件,液压部件', 0.8, 'AVAILABLE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_line" ("id", "space_code", "line_code", "line_name", "supported_categories", "efficiency_rate", "status", "created_at", "updated_at") VALUES ('LINE-0003', 'AI-DECISION-SIM', 'LINE-0003', '三号检修产线', '机械部件,电机部件,液压部件,精密部件', 0.6, 'MAINTENANCE', '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 9. 产能时段(未来3天 x 3产线 x 上下班2时段)
CREATE TABLE production_capacity_slot (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "line_id" character varying(64) NOT NULL,
    "slot_start" timestamp NOT NULL,
    "slot_end" timestamp NOT NULL,
    "available_minutes" integer NOT NULL,
    "used_minutes" integer NOT NULL,
    "reserved_minutes" integer NOT NULL,
    "capacity_status" character varying(16) NOT NULL,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT production_capacity_slot_pkey PRIMARY KEY ("id")
);
COMMENT ON TABLE "production_capacity_slot" IS '产能时段(AI决策模拟库-产能)';
COMMENT ON COLUMN "production_capacity_slot"."line_id" IS '产线ID(与 production_line.id 对齐)';
COMMENT ON COLUMN "production_capacity_slot"."slot_start" IS '时段开始';
COMMENT ON COLUMN "production_capacity_slot"."slot_end" IS '时段结束';
COMMENT ON COLUMN "production_capacity_slot"."available_minutes" IS '可用分钟数';
COMMENT ON COLUMN "production_capacity_slot"."used_minutes" IS '已使用分钟数';
COMMENT ON COLUMN "production_capacity_slot"."reserved_minutes" IS '已预留分钟数';
COMMENT ON COLUMN "production_capacity_slot"."capacity_status" IS '状态: OPEN/FULL/CLOSED';

INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0001', 'AI-DECISION-SIM', 'LINE-0001', '2026-09-16 08:00:00', '2026-09-16 12:00:00', 240, 0, 240, 'FULL', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0002', 'AI-DECISION-SIM', 'LINE-0001', '2026-09-16 13:00:00', '2026-09-16 17:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0003', 'AI-DECISION-SIM', 'LINE-0001', '2026-09-17 08:00:00', '2026-09-17 12:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0004', 'AI-DECISION-SIM', 'LINE-0001', '2026-09-17 13:00:00', '2026-09-17 17:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0005', 'AI-DECISION-SIM', 'LINE-0001', '2026-09-18 08:00:00', '2026-09-18 12:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0006', 'AI-DECISION-SIM', 'LINE-0001', '2026-09-18 13:00:00', '2026-09-18 17:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0007', 'AI-DECISION-SIM', 'LINE-0002', '2026-09-16 08:00:00', '2026-09-16 12:00:00', 240, 230, 0, 'FULL', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0008', 'AI-DECISION-SIM', 'LINE-0002', '2026-09-16 13:00:00', '2026-09-16 17:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0009', 'AI-DECISION-SIM', 'LINE-0002', '2026-09-17 08:00:00', '2026-09-17 12:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0010', 'AI-DECISION-SIM', 'LINE-0002', '2026-09-17 13:00:00', '2026-09-17 17:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0011', 'AI-DECISION-SIM', 'LINE-0002', '2026-09-18 08:00:00', '2026-09-18 12:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0012', 'AI-DECISION-SIM', 'LINE-0002', '2026-09-18 13:00:00', '2026-09-18 17:00:00', 240, 0, 0, 'OPEN', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0013', 'AI-DECISION-SIM', 'LINE-0003', '2026-09-16 08:00:00', '2026-09-16 12:00:00', 240, 0, 0, 'CLOSED', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0014', 'AI-DECISION-SIM', 'LINE-0003', '2026-09-16 13:00:00', '2026-09-16 17:00:00', 240, 0, 0, 'CLOSED', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0015', 'AI-DECISION-SIM', 'LINE-0003', '2026-09-17 08:00:00', '2026-09-17 12:00:00', 240, 0, 0, 'CLOSED', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0016', 'AI-DECISION-SIM', 'LINE-0003', '2026-09-17 13:00:00', '2026-09-17 17:00:00', 240, 0, 0, 'CLOSED', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0017', 'AI-DECISION-SIM', 'LINE-0003', '2026-09-18 08:00:00', '2026-09-18 12:00:00', 240, 0, 0, 'CLOSED', '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "production_capacity_slot" ("id", "space_code", "line_id", "slot_start", "slot_end", "available_minutes", "used_minutes", "reserved_minutes", "capacity_status", "created_at", "updated_at") VALUES ('CAP-0018', 'AI-DECISION-SIM', 'LINE-0003', '2026-09-18 13:00:00', '2026-09-18 17:00:00', 240, 0, 0, 'CLOSED', '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 10. 订单事件(ORDER_CREATED)
CREATE TABLE business_event (
    "id" character varying(64) NOT NULL,
    "space_code" character varying(64) NOT NULL,
    "event_type" character varying(64) NOT NULL,
    "aggregate_type" character varying(64) NOT NULL,
    "aggregate_id" character varying(64) NOT NULL,
    "event_time" timestamp NOT NULL,
    "payload_json" jsonb NOT NULL,
    "source_system" character varying(64) NOT NULL,
    "process_status" character varying(32) NOT NULL,
    "retry_count" integer NOT NULL,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NOT NULL,
    CONSTRAINT business_event_pkey PRIMARY KEY ("id")
);
COMMENT ON TABLE "business_event" IS '订单事件(AI决策模拟库-事件)';
COMMENT ON COLUMN "business_event"."event_type" IS '事件类型: ORDER_CREATED';
COMMENT ON COLUMN "business_event"."aggregate_type" IS '聚合类型: SALES_ORDER';
COMMENT ON COLUMN "business_event"."aggregate_id" IS '聚合ID(与 sales_order.id 对齐)';
COMMENT ON COLUMN "business_event"."event_time" IS '事件发生时间';
COMMENT ON COLUMN "business_event"."payload_json" IS '事件完整内容(JSON)';
COMMENT ON COLUMN "business_event"."process_status" IS '处理状态: PENDING/PROCESSING/PROCESSED/FAILED';
COMMENT ON COLUMN "business_event"."retry_count" IS '重试次数';

INSERT INTO "business_event" ("id", "space_code", "event_type", "aggregate_type", "aggregate_id", "event_time", "payload_json", "source_system", "process_status", "retry_count", "created_at", "updated_at") VALUES ('EVT-20260915-0001', 'AI-DECISION-SIM', 'ORDER_CREATED', 'SALES_ORDER', 'ORD-20260915-0001', '2026-09-15 08:30:00', '{"eventId": "EVT-20260915-0001", "eventType": "ORDER_CREATED", "aggregateType": "SALES_ORDER", "aggregateId": "ORD-20260915-0001", "occurredAt": "2026-09-15 08:30:00+08:00", "sourceSystem": "MOCK-ERP", "payload": {"orderNo": "ORD-20260915-0001", "customerId": "CUST-0002", "orderPriority": 70, "requestedDeliveryTime": "2026-09-25 09:00:00+08:00"}}'::jsonb, 'MOCK-ERP', 'PENDING', 0, '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "business_event" ("id", "space_code", "event_type", "aggregate_type", "aggregate_id", "event_time", "payload_json", "source_system", "process_status", "retry_count", "created_at", "updated_at") VALUES ('EVT-20260915-0002', 'AI-DECISION-SIM', 'ORDER_CREATED', 'SALES_ORDER', 'ORD-20260915-0002', '2026-09-15 08:40:00', '{"eventId": "EVT-20260915-0002", "eventType": "ORDER_CREATED", "aggregateType": "SALES_ORDER", "aggregateId": "ORD-20260915-0002", "occurredAt": "2026-09-15 08:40:00+08:00", "sourceSystem": "MOCK-ERP", "payload": {"orderNo": "ORD-20260915-0002", "customerId": "CUST-0003", "orderPriority": 60, "requestedDeliveryTime": "2026-09-28 09:00:00+08:00"}}'::jsonb, 'MOCK-ERP', 'PENDING', 0, '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "business_event" ("id", "space_code", "event_type", "aggregate_type", "aggregate_id", "event_time", "payload_json", "source_system", "process_status", "retry_count", "created_at", "updated_at") VALUES ('EVT-20260915-0003', 'AI-DECISION-SIM', 'ORDER_CREATED', 'SALES_ORDER', 'ORD-20260915-0003', '2026-09-15 08:50:00', '{"eventId": "EVT-20260915-0003", "eventType": "ORDER_CREATED", "aggregateType": "SALES_ORDER", "aggregateId": "ORD-20260915-0003", "occurredAt": "2026-09-15 08:50:00+08:00", "sourceSystem": "MOCK-ERP", "payload": {"orderNo": "ORD-20260915-0003", "customerId": "CUST-0001", "orderPriority": 100, "requestedDeliveryTime": "2026-09-16 17:00:00+08:00"}}'::jsonb, 'MOCK-ERP', 'PENDING', 0, '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "business_event" ("id", "space_code", "event_type", "aggregate_type", "aggregate_id", "event_time", "payload_json", "source_system", "process_status", "retry_count", "created_at", "updated_at") VALUES ('EVT-20260915-0004', 'AI-DECISION-SIM', 'ORDER_CREATED', 'SALES_ORDER', 'ORD-20260915-0004', '2026-09-15 08:55:00', '{"eventId": "EVT-20260915-0004", "eventType": "ORDER_CREATED", "aggregateType": "SALES_ORDER", "aggregateId": "ORD-20260915-0004", "occurredAt": "2026-09-15 08:55:00+08:00", "sourceSystem": "MOCK-ERP", "payload": {"orderNo": "ORD-20260915-0004", "customerId": "CUST-0004", "orderPriority": 50, "requestedDeliveryTime": "2026-09-22 09:00:00+08:00"}}'::jsonb, 'MOCK-ERP', 'PENDING', 0, '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "business_event" ("id", "space_code", "event_type", "aggregate_type", "aggregate_id", "event_time", "payload_json", "source_system", "process_status", "retry_count", "created_at", "updated_at") VALUES ('EVT-20260915-0005', 'AI-DECISION-SIM', 'ORDER_CREATED', 'SALES_ORDER', 'ORD-20260915-0005', '2026-09-15 08:57:00', '{"eventId": "EVT-20260915-0005", "eventType": "ORDER_CREATED", "aggregateType": "SALES_ORDER", "aggregateId": "ORD-20260915-0005", "occurredAt": "2026-09-15 08:57:00+08:00", "sourceSystem": "MOCK-ERP", "payload": {"orderNo": "ORD-20260915-0005", "customerId": "CUST-0005", "orderPriority": 30, "requestedDeliveryTime": "2026-09-17 09:00:00+08:00"}}'::jsonb, 'MOCK-ERP', 'PENDING', 0, '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "business_event" ("id", "space_code", "event_type", "aggregate_type", "aggregate_id", "event_time", "payload_json", "source_system", "process_status", "retry_count", "created_at", "updated_at") VALUES ('EVT-20260915-0006', 'AI-DECISION-SIM', 'ORDER_CREATED', 'SALES_ORDER', 'ORD-20260915-0006', '2026-09-15 08:59:00', '{"eventId": "EVT-20260915-0006", "eventType": "ORDER_CREATED", "aggregateType": "SALES_ORDER", "aggregateId": "ORD-20260915-0006", "occurredAt": "2026-09-15 08:59:00+08:00", "sourceSystem": "MOCK-ERP", "payload": {"orderNo": "ORD-20260915-0006", "customerId": "CUST-0002", "orderPriority": 70, "requestedDeliveryTime": "2026-09-23 09:00:00+08:00"}}'::jsonb, 'MOCK-ERP', 'PENDING', 0, '2026-09-15 09:00:00', '2026-09-15 09:00:00');
INSERT INTO "business_event" ("id", "space_code", "event_type", "aggregate_type", "aggregate_id", "event_time", "payload_json", "source_system", "process_status", "retry_count", "created_at", "updated_at") VALUES ('EVT-20260915-0007', 'AI-DECISION-SIM', 'ORDER_CREATED', 'SALES_ORDER', 'ORD-20260915-0006', '2026-09-15 08:59:00', '{"eventId": "EVT-20260915-0006", "eventType": "ORDER_CREATED", "aggregateType": "SALES_ORDER", "aggregateId": "ORD-20260915-0006", "occurredAt": "2026-09-15 08:59:00+08:00", "sourceSystem": "MOCK-ERP", "payload": {"orderNo": "ORD-20260915-0006", "customerId": "CUST-0002", "orderPriority": 70, "requestedDeliveryTime": "2026-09-23 09:00:00+08:00"}}'::jsonb, 'MOCK-ERP', 'PENDING', 0, '2026-09-15 09:00:00', '2026-09-15 09:00:00');

-- 11. 统一设置 owner = datamaster (应用连接账号), 保证 app 可读写
ALTER TABLE "customer" OWNER TO datamaster;
ALTER TABLE "product" OWNER TO datamaster;
ALTER TABLE "material" OWNER TO datamaster;
ALTER TABLE "product_material" OWNER TO datamaster;
ALTER TABLE "sales_order" OWNER TO datamaster;
ALTER TABLE "sales_order_item" OWNER TO datamaster;
ALTER TABLE "material_inventory" OWNER TO datamaster;
ALTER TABLE "production_line" OWNER TO datamaster;
ALTER TABLE "production_capacity_slot" OWNER TO datamaster;
ALTER TABLE "business_event" OWNER TO datamaster;
