# AI 决策审批模拟数据规范

## 1. 目标

本规范用于生成“新订单自动排产”的模拟数据，验证以下完整流程：

```text
订单创建事件
  -> AI 查询订单、库存、产能和设备状态
  -> 生成排产决策草案
  -> 人工审批
  -> 审批通过后执行写入
```

重要约束：

- AI 决策阶段只能读取模拟业务数据。
- AI 只能生成排产建议，不得修改订单、库存或生产计划。
- 审批前不得创建正式生产计划。
- 审批通过后，系统才允许把决策草案转换为正式生产计划。

## 2. 数据范围

第一版只需要生成以下 10 类数据：

| 数据类型 | 用途 | 是否必须 |
|---|---|---|
| 客户 | 订单归属和优先级来源 | 是 |
| 产品 | 订单和生产对象 | 是 |
| 物料 | 生产所需原材料 | 是 |
| 产品物料清单 | 计算订单所需物料 | 是 |
| 订单 | 触发 AI 决策的业务数据 | 是 |
| 订单明细 | 产品和数量 | 是 |
| 库存 | 判断物料是否足够 | 是 |
| 生产线 | 可执行生产的设备或产线 | 是 |
| 生产能力时段 | 判断时间和产能冲突 | 是 |
| 订单事件 | 触发流程 | 是 |

正式生产计划不属于输入数据。它必须在人工审批通过后由动作流程生成。

## 3. 通用字段规范

所有表建议包含以下公共字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar(64) | 全局唯一 ID，建议使用有业务含义的前缀 |
| space_code | varchar(64) | DataMaster 空间编码，所有数据使用同一个模拟空间 |
| created_at | timestamp | 创建时间，统一使用 `Asia/Shanghai` |
| updated_at | timestamp | 更新时间 |
| status | varchar(32) | 业务状态 |

建议 ID 格式：

```text
CUST-0001
PROD-0001
MAT-0001
ORD-20260915-0001
LINE-0001
CAP-20260915-0001
EVT-20260915-0001
```

## 4. 基础业务数据

### 4.1 客户表：customer

| 字段 | 类型 | 约束/示例 |
|---|---|---|
| id | varchar(64) | 主键，如 `CUST-0001` |
| customer_code | varchar(64) | 唯一 |
| customer_name | varchar(128) | 必填 |
| customer_level | varchar(16) | `A`、`B`、`C` |
| delivery_priority | int | 1-100，数值越大优先级越高 |
| default_delivery_days | int | 默认交期天数 |
| status | varchar(16) | `ACTIVE`、`INACTIVE` |

至少生成 5 个客户，其中：

- 1 个 A 级客户，优先级 100。
- 2 个 B 级客户，优先级 60-80。
- 2 个 C 级客户，优先级 30-50。

### 4.2 产品表：product

| 字段 | 类型 | 约束/示例 |
|---|---|---|
| id | varchar(64) | 主键，如 `PROD-0001` |
| product_code | varchar(64) | 唯一 |
| product_name | varchar(128) | 必填 |
| product_category | varchar(64) | 产品分类 |
| unit | varchar(16) | `件`、`台`、`套` |
| standard_minutes | decimal(12,2) | 每件标准生产分钟数 |
| min_batch_qty | int | 最小生产批量 |
| active_flag | boolean | 是否可生产 |

至少生成 4 个产品：

| 产品 | 标准工时 | 最小批量 |
|---|---:|---:|
| PROD-0001 | 10 分钟 | 10 |
| PROD-0002 | 20 分钟 | 5 |
| PROD-0003 | 35 分钟 | 5 |
| PROD-0004 | 60 分钟 | 1 |

### 4.3 物料表：material

| 字段 | 类型 | 约束/示例 |
|---|---|---|
| id | varchar(64) | 主键，如 `MAT-0001` |
| material_code | varchar(64) | 唯一 |
| material_name | varchar(128) | 必填 |
| unit | varchar(16) | `件`、`kg`、`米` |
| safety_stock | decimal(12,2) | 安全库存 |
| lead_time_days | int | 采购提前期 |
| status | varchar(16) | `ACTIVE`、`INACTIVE` |

至少生成 5 个物料，其中至少 1 个物料库存不足，用于测试异常场景。

### 4.4 产品物料清单：product_material

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar(64) | 主键 |
| product_id | varchar(64) | 关联 `product.id` |
| material_id | varchar(64) | 关联 `material.id` |
| quantity_per_unit | decimal(12,4) | 生产 1 个产品所需数量 |
| scrap_rate | decimal(8,4) | 损耗率，取 0-0.2 |

约束：

```text
product_id + material_id 唯一
quantity_per_unit > 0
0 <= scrap_rate <= 0.2
```

## 5. 订单数据

### 5.1 订单表：sales_order

| 字段 | 类型 | 约束/示例 |
|---|---|---|
| id | varchar(64) | 主键，如 `ORD-20260915-0001` |
| order_no | varchar(64) | 唯一 |
| customer_id | varchar(64) | 关联客户 |
| order_status | varchar(32) | `CREATED`、`CONFIRMED`、`CANCELLED`、`COMPLETED` |
| order_priority | int | 1-100 |
| order_time | timestamp | 下单时间 |
| requested_delivery_time | timestamp | 要求交付时间 |
| source_system | varchar(64) | 如 `MOCK-ERP` |
| remark | varchar(500) | 备注 |

订单优先级计算建议：

```text
order_priority = max(customer.delivery_priority, 手工指定优先级)
```

### 5.2 订单明细表：sales_order_item

| 字段 | 类型 | 约束/示例 |
|---|---|---|
| id | varchar(64) | 主键 |
| order_id | varchar(64) | 关联 `sales_order.id` |
| product_id | varchar(64) | 关联 `product.id` |
| quantity | int | 大于 0 |
| delivered_quantity | int | 默认 0 |
| unit_price | decimal(12,2) | 可选 |

约束：

```text
quantity > 0
0 <= delivered_quantity <= quantity
```

## 6. 库存和产能数据

### 6.1 物料库存表：material_inventory

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar(64) | 主键 |
| material_id | varchar(64) | 关联物料 |
| warehouse_code | varchar(64) | 仓库编码 |
| available_quantity | decimal(12,2) | 可用库存 |
| reserved_quantity | decimal(12,2) | 已预留库存 |
| inbound_quantity | decimal(12,2) | 在途库存 |
| inventory_time | timestamp | 库存快照时间 |

可用库存计算：

```text
net_available = available_quantity - reserved_quantity
```

AI 排产只能使用 `net_available`，不能把在途库存当作当前可用库存，除非决策结果明确标记风险。

### 6.2 生产线表：production_line

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar(64) | 主键，如 `LINE-0001` |
| line_code | varchar(64) | 唯一 |
| line_name | varchar(128) | 产线名称 |
| supported_categories | varchar(500) | 支持的产品类别，JSON 或逗号分隔 |
| efficiency_rate | decimal(8,4) | 产线效率，0-1 |
| status | varchar(16) | `AVAILABLE`、`MAINTENANCE`、`DISABLED` |

至少生成 3 条产线：

- 1 条高效率产线。
- 1 条普通产线。
- 1 条维护中的产线，不允许被排产。

### 6.3 产能时段表：production_capacity_slot

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar(64) | 主键 |
| line_id | varchar(64) | 关联产线 |
| slot_start | timestamp | 时段开始 |
| slot_end | timestamp | 时段结束 |
| available_minutes | int | 可用分钟数 |
| used_minutes | int | 已使用分钟数 |
| reserved_minutes | int | 已预留分钟数 |
| capacity_status | varchar(16) | `OPEN`、`FULL`、`CLOSED` |

可用产能计算：

```text
remaining_minutes = available_minutes - used_minutes - reserved_minutes
```

约束：

```text
slot_start < slot_end
available_minutes >= 0
used_minutes >= 0
reserved_minutes >= 0
remaining_minutes >= 0
```

至少生成未来 3 天、每条产线每天 2 个时段：

- 上午：08:00-12:00。
- 下午：13:00-17:00。

## 7. 订单事件数据

### 7.1 订单事件表：business_event

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar(64) | 主键，如 `EVT-20260915-0001` |
| event_type | varchar(64) | `ORDER_CREATED` |
| aggregate_type | varchar(64) | `SALES_ORDER` |
| aggregate_id | varchar(64) | 订单 ID |
| event_time | timestamp | 事件发生时间 |
| payload_json | json/text | 事件完整内容 |
| source_system | varchar(64) | `MOCK-ERP` |
| process_status | varchar(32) | `PENDING`、`PROCESSING`、`PROCESSED`、`FAILED` |
| retry_count | int | 默认 0 |

事件示例：

```json
{
  "eventId": "EVT-20260915-0001",
  "eventType": "ORDER_CREATED",
  "aggregateType": "SALES_ORDER",
  "aggregateId": "ORD-20260915-0001",
  "occurredAt": "2026-09-15T09:30:00+08:00",
  "sourceSystem": "MOCK-ERP",
  "payload": {
    "orderNo": "ORD-20260915-0001",
    "customerId": "CUST-0001",
    "orderPriority": 100,
    "requestedDeliveryTime": "2026-09-16T17:00:00+08:00"
  }
}
```

约束：

- `event_id` 必须幂等，重复消费不能生成重复决策。
- `aggregate_id` 必须能关联到一条有效订单。
- 事件时间不能早于订单创建时间。

## 8. 推荐测试场景

至少生成 6 个订单，覆盖以下场景：

| 场景 | 数据要求 | 预期结果 |
|---|---|---|
| 正常排产 | 库存足够、产能足够、交期宽松 | 生成可执行排产草案，等待审批 |
| 高优先级订单 | A 级客户、交期紧 | 草案优先分配高效率产线 |
| 产能冲突 | 可用产能不足 | 草案标记冲突，不得直接执行 |
| 物料不足 | 某物料 `net_available` 小于需求 | 草案标记物料风险，建议延期或补料 |
| 多订单竞争 | 两个订单争用同一时段 | AI 给出排序理由和候选方案 |
| 重复事件 | 同一个订单事件发送两次 | 只能生成一份决策草案 |

建议订单分布：

- 2 个正常订单。
- 1 个高优先级紧急订单。
- 1 个库存不足订单。
- 1 个产能不足订单。
- 1 个重复事件订单。

## 9. AI 决策结果规范

AI 输出必须是结构化 JSON，不能只返回自然语言。

```json
{
  "decisionId": "DEC-20260915-0001",
  "decisionType": "PRODUCTION_SCHEDULING",
  "sourceEventId": "EVT-20260915-0001",
  "sourceOrderId": "ORD-20260915-0001",
  "status": "WAITING_APPROVAL",
  "confidence": 0.93,
  "requiresApproval": true,
  "summary": "建议将订单拆分到两条可用产线生产，以满足交期。",
  "risks": [],
  "scheduleItems": [
    {
      "orderItemId": "ITEM-20260915-0001",
      "productId": "PROD-0001",
      "lineId": "LINE-0001",
      "slotStart": "2026-09-16T08:00:00+08:00",
      "slotEnd": "2026-09-16T11:20:00+08:00",
      "quantity": 20,
      "reason": "产线可用产能充足，且满足客户交期。"
    }
  ],
  "validation": {
    "inventoryPassed": true,
    "capacityPassed": true,
    "deliveryTimePassed": true,
    "conflictPassed": true
  }
}
```

注意：

- `status` 初始必须为 `WAITING_APPROVAL`。
- `requiresApproval` 第一版固定为 `true`。
- `scheduleItems` 只是候选方案，不是正式生产计划。
- 审批前不能写入正式 `production_plan` 表。

## 10. 审批执行约束

审批通过后，执行前必须重新校验：

1. 订单版本是否变化。
2. 库存是否仍然足够。
3. 产线是否仍然可用。
4. 产能时段是否仍然空闲。
5. 决策草案是否已过期。

任一条件不满足，都必须把决策标记为 `EXPIRED` 或 `REPLAN_REQUIRED`，不能直接写入生产计划。

## 11. 第一版生成规模

为了便于调试，第一版不要生成过多数据：

```text
空间：1 个
客户：5 个
产品：4 个
物料：5 个
产品物料关系：10-15 条
产线：3 条
产能时段：18 条以上
订单：6 个
订单明细：8-12 条
订单事件：6-7 条
```

先确保 6 个测试场景全部跑通，再扩大到 1000 个订单测试性能和并发。

## 12. 不要生成的内容

模拟数据生成阶段不要创建或模拟以下内容：

- 已审批的生产计划。
- 已执行的库存扣减。
- 已下发的设备指令。
- 没有来源订单的生产任务。
- 没有来源事件的决策结果。
- 直接把 AI 自然语言写入业务表的字段。

这些内容必须由“人工审批通过后的动作流程”产生。
