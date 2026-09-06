# Action 动作执行引擎设计

> 用 Spring Boot 落地 Palantir Ontology「Action(动作)」层的一种实现方案:让本体从「只读描述」变成「可写操作面」。
> 目标栈:JDK8 / Spring Boot 2.5.5(与现有平台一致)。
> 核心机制:**变更清单挂起 → 审批 → 原子提交**。不依赖 AI,人点按钮 / 表单提交 / API 调用即可触发。

---

## 0. 设计目标

一个 Action(如"审批订单""调价""标记预警")从触发到落库,要满足:

| 目标 | 手段 |
|---|---|
| **原子性** | 一个 Action 改多个对象,要么全成、要么全不改 |
| **可校验** | 提交前统一跑权限 + 前置条件 + 本体约束,脏数据挡在门外 |
| **可审批** | 变更先"挂起"成提案,人工确认后才真正写库 |
| **可审计/血缘** | 谁、何时、因哪个 Action、改了什么,全程留痕 |
| **可回滚** | 记录旧值,失败或撤销时可反向恢复 |

**关键原则**:Action 逻辑只"计算要改什么"(产出变更清单),绝不直接写库;写库这一步被推迟到校验/审批之后,并打包成原子事务。

---

## 1. 触发来源(无需 AI)

Action 有四种触发入口,AI 只是最后一种,**去掉 AI 前三种照常工作**:

```
① 人点按钮      用户在界面点"审批"/"调价"按钮        ← 最主流
② 表单提交      用户填参数表单,提交即触发
③ 程序/API 调用 其他系统 API 触发,或定时任务批量触发
④ AI 触发       LLM 填参数并触发(2023 后可选,机制同上)
```

AI 的作用仅是把"人去界面找按钮、填参数"换成"自然语言说一句,由 LLM 挑 Action、填参数"。**执行机制完全一致。**

---

## 1.5 两类 Action:声明式(无代码) vs 函数式(要代码)

不是所有 Action 都要写代码。按逻辑复杂度分两层:

### 层1:声明式变更 —— 零业务代码

简单 CRUD 型操作,纯配置就够:

```
把 订单.状态 改成 "已审"
创建一个 预警 对象,链到 供应商
删除 某条关系
```

在界面上配好"改哪个字段、改成什么(或取哪个参数)"即可。**多数业务操作(改状态、建关系、更新字段)属于这层,新增动作只加一条配置,不写业务代码。**

### 层2:函数式 —— 需要写代码(Java Handler)

当逻辑复杂到声明式表达不了时才写代码:

- 要**计算**:"新库存 = 当前库存 − 订单量 × 系数"
- 要**条件分支**:"风险分>80 就冻结,否则只标记"
- 要**循环/批量**:"给该供应商关联的所有订单打标"
- 要**跨对象聚合**:"汇总子订单金额写到主订单"

这时写一个 `ActionHandler` 实现类,在 `buildEdits` 里手写 Java 逻辑(见第 3 节)。

> **关于 TS/Python**:Palantir 的层2 用 Functions(TypeScript 为主,Python 用于数据科学类逻辑),提前编译注册、运行时传参调用,函数只返回变更清单不碰库。**本方案不引入 TS/Python 运行时——`ActionHandler.buildEdits()`(Java)就是等价物,Java 就是我们的"Functions 语言"。** Palantir 用 TS/Python 是为了给非 Java 的数据科学家/前端一个低门槛环境;Java 团队直接用 Java Handler 更合理。

| | 层1 声明式 | 层2 函数式 |
|---|---|---|
| 适用 | 改字段/建关系/删关系 | 计算/分支/循环/聚合 |
| 实现 | 加一条配置 | 写一个 `ActionHandler` 类 |
| 代码量 | 零业务代码 | 手写 Java 逻辑 |
| 共用引擎 | `ConfigurableActionHandler`(见 3.5) | 各自的 Handler |

---

## 2. 数据模型

### 2.1 action_request(提案主表)

一次 Action 触发 = 一条记录。

| 字段 | 说明 |
|---|---|
| id | 主键 |
| action_type | 动作类型:审批订单 / 调价 / 标记预警 |
| params | JSON,触发时填的参数 `{orderId:123, price:100}` |
| edits | JSON,产出的变更清单(核心,见 2.3) |
| status | PENDING / APPROVED / REJECTED / EXECUTED / FAILED |
| created_by / create_time | 发起人 |
| approved_by / approve_time | 审批人 |
| error_msg | 失败原因 |

### 2.2 action_audit_log(审计日志)

每一步(SUBMIT / APPROVE / REJECT / EXECUTED / FAILED)都记:谁、何时、对哪个 request、做了什么。

### 2.3 edits 字段:变更清单(灵魂)

一批结构化的变更描述,即"购物车":

```json
[
  {"target":"Order:123", "field":"status",   "old":"待审", "new":"已审"},
  {"target":"Order:123", "field":"approver", "old":null,   "new":"张三"}
]
```

**必须记录 `old`(旧值)**:用于回滚、审计、审批时展示 diff。

---

## 3. 核心接口:定义 Action 骨架

```java
public interface ActionHandler {
    String actionType();
    void validate(ActionContext ctx);
    List<EditItem> buildEdits(ActionContext ctx);
    default void afterCommit(ActionContext ctx) {}
}
```

- `validate`:前置条件校验(如订单必须=待审)。
- `buildEdits`:**只计算要改什么,返回变更清单,绝不碰数据库**。这是整套设计安全的根。
- `afterCommit`:副作用(调外部系统),仅在业务库提交成功后执行。

### 3.5 声明式引擎:ConfigurableActionHandler

层1(声明式)的 Action 不为每个动作写类,而是共用一个**配置驱动的通用 Handler**。它本质是一个**翻译器**:把「本体层的动作声明」翻译成「存储层的数据库操作」。

**引擎的本质 —— 两跳翻译:**

```
动作声明  ──①──►  变更清单(EditItem)  ──②──►  数据库操作
         填参数、定位              把每条变更映射成
         对象/字段                UPDATE/INSERT/DELETE
   (还在本体语义层)                  (落到物理层)
```

- **① buildEdits**:把声明 + 参数,翻译成一批"要改什么"(对象、字段、新旧值)——仍是本体语义层。
- **② applyEdit**:把每条变更翻译成真正的存储操作——落到物理层(见 4.3)。
- 中间的"变更清单"不是多余:它是插入**校验、审批、审计、原子提交**的挂载点。

**配置(界面配出来,存库/JSON):**

```json
{
  "actionType": "审批订单",
  "kind": "DECLARATIVE",
  "edits": [
    {"op":"UPDATE", "target":"Order:${orderId}", "field":"status",   "value":"已审"},
    {"op":"UPDATE", "target":"Order:${orderId}", "field":"approver", "value":"${user}"}
  ]
}
```

`${orderId}` `${user}` 是占位符,触发时从参数/上下文填。

**通用 Handler(读配置 → 填占位符 → 产出变更清单,仍不写库):**

```java
@Component
public class ConfigurableActionHandler implements ActionHandler {

    public String actionType() { return "*"; }   // 通吃所有声明式 Action

    public List<EditItem> buildEdits(ActionContext ctx) {
        ActionConfig cfg = configRepo.get(ctx.getActionType());   // 取该动作的配置
        List<EditItem> edits = new ArrayList<>();
        for (EditRule rule : cfg.getEdits()) {
            String target = resolve(rule.getTarget(), ctx);       // Order:${orderId} → Order:123
            Object value  = resolve(rule.getValue(), ctx);        // ${user} → 张三
            Object old    = entityService.getField(target, rule.getField());  // 取旧值
            edits.add(new EditItem(rule.getOp(), target, rule.getField(), old, value));
        }
        return edits;
    }
}
```

**要点:** 所谓"引擎"不是现成产品或开源框架,就是**你自己写一次的这个通用 Handler + `applyEdit`**。抽它出来的意义:简单 Action 有几十上百个,新增时只加一条配置、不写代码——这才是"零代码"的真正含义(零业务代码,引擎本身写一次)。

> **上层不碰物理存储**:声明"订单.状态=已审"即可,不用知道它存哪张表 / 哪个 ES 索引 / 哪个字段名。物理存储换了(MySQL→ES→Nebula),只改 `applyEdit`,上层声明不动。这与本平台"可读名优先、property_id 挡在内部"的思路一致。

---

## 4. 执行引擎:两阶段

### 4.1 阶段1 —— 发起(产出提案,挂起,不写业务库)

```java
public ActionRequest submit(String actionType, Map<String,Object> params, String user) {
    ActionHandler handler = registry.get(actionType);
    ActionContext ctx = new ActionContext(params, user);

    handler.validate(ctx);                       // 前置校验
    checkPermission(actionType, user, ctx);       // 权限校验
    List<EditItem> edits = handler.buildEdits(ctx);   // 只算变更,不写库

    ActionRequest req = new ActionRequest();
    req.setActionType(actionType);
    req.setParams(toJson(params));
    req.setEdits(toJson(edits));

    if (needApproval(actionType)) {
        req.setStatus(PENDING);                   // ★挂起,业务库纹丝不动
    } else {
        req.setStatus(APPROVED);
    }
    requestRepo.save(req);
    audit(req, "SUBMIT", user);

    if (req.getStatus() == APPROVED) {
        commit(req.getId(), user);                // 无需审批的直接进阶段2
    }
    return req;
}
```

### 4.2 阶段2 —— 提交(审批通过那一刻,真正写库)

```java
@Transactional  // ★原子性:要么全改,要么全回滚
public void commit(Long requestId, String operator) {
    ActionRequest req = requestRepo.findById(requestId);
    if (req.getStatus() != APPROVED)
        throw new IllegalStateException("未批准不能提交");

    List<EditItem> edits = parseEdits(req.getEdits());
    try {
        for (EditItem e : edits) {
            applyEdit(e);                         // ★这里才真正写业务库/ES/图库
        }
        req.setStatus(EXECUTED);
        requestRepo.save(req);
        audit(req, "EXECUTED", operator);

        registry.get(req.getActionType())
                .afterCommit(ctxOf(req));         // 提交成功后才调外部系统
    } catch (Exception ex) {
        req.setStatus(FAILED);
        req.setErrorMsg(ex.getMessage());
        requestRepo.save(req);
        throw ex;                                 // 抛出 → 事务回滚已写部分
    }
}
```

### 4.3 applyEdit —— 把一条变更落到存储

```java
private void applyEdit(EditItem e) {
    // op 字段决定翻译成哪种存储操作(第②跳翻译)
    switch (e.getOp()) {
        case "UPDATE":
            // 关系型库:UPDATE target_type SET field=new WHERE id=target_id
            // ES + 图库:调实体更新服务,更新指定字段
            entityService.updateField(e.getTarget(), e.getField(), e.getNew());
            break;
        case "INSERT":
            entityService.create(e.getTarget(), e.getFields());
            break;
        case "DELETE":
            entityService.delete(e.getTarget());
            break;
        default:
            throw new IllegalArgumentException("未知操作:" + e.getOp());
    }
}
```

---

## 5. 审批接口:审批即触发 commit

```java
@PostMapping("/action/{id}/approve")
public RestResult approve(@PathVariable Long id, @RequestParam String opinion) {
    ActionRequest req = requestRepo.findById(id);
    checkApprovePermission(req, currentUser());

    req.setStatus(APPROVED);
    req.setApprovedBy(currentUser());
    requestRepo.save(req);
    audit(req, "APPROVE", currentUser());

    actionEngine.commit(id, currentUser());       // ★审批通过 → 立刻真正执行
    return RestResult.success();
}

@PostMapping("/action/{id}/reject")
public RestResult reject(@PathVariable Long id, @RequestParam String reason) {
    ActionRequest req = requestRepo.findById(id);
    req.setStatus(REJECTED);                       // 作废,业务库始终没动过
    requestRepo.save(req);
    audit(req, "REJECT", currentUser());
    return RestResult.success();
}
```

---

## 6. 完整时序(以"调价需审批"为例)

```
① 李四 POST /action/submit {type:调价, price:100}
      → validate + 权限 + buildEdits → edits=[X.价格 80→100]
      → status=PENDING 存库              ★业务库里 X 还是 80

② 王五 GET /action/pending              → 看到提案预览 "80→100"

③ 王五 POST /action/{id}/approve
      → status=APPROVED
      → commit():@Transactional 内 applyEdit → ★X 真的变成 100
                 + afterCommit() 调价格系统 API
                 + audit 记录 "王五 批准 于 xx:xx"
      → status=EXECUTED
```

- 审批前:变更清单只是"提案",数据库纹丝不动。
- 审批通过的那一刻:引擎把攒好的清单原子提交进数据库 + 触发外部副作用 = "真正执行"。

---

## 7. 状态机

```
              submit
                │
        ┌───────┴────────┐
   需审批│                │不需审批
        ▼                ▼
     PENDING          APPROVED ──commit──► EXECUTED
      │    │                                  ▲
 approve  reject                              │
      │    │                          (无需审批直接提交)
      ▼    ▼
  APPROVED REJECTED
      │
   commit
      ▼
   EXECUTED / FAILED(提交异常)
```

---

## 8. 落地要点(踩坑预防)

1. **原子性靠 `@Transactional`**。若底层是 ES / 图库等非事务型存储,跨库原子性做不到 —— 补偿方案:记录 `old_value`,失败时反向 `applyEdit` 回滚;或用分布式锁 + 状态机保证不重复提交。
2. **旧值(old_value)必须存**。用于审计、回滚、审批时展示 diff。
3. **幂等**:`commit` 防重复执行(status 已 EXECUTED 直接返回);外部 API 调用带幂等键。
4. **副作用放 `afterCommit`**,且业务库提交成功后再调 —— 否则库没改成功却通知了下游,数据不一致。
5. **统一路径**:无审批的 Action 走同一套代码,只是 `submit` 里直接 `commit`,不为"要不要审批"分两套逻辑。
6. **权限**:发起(submit)和审批(approve)是两类权限,分别校验。

---

## 9. 一句话总结

这套设计 = **一张 `action_request` 表存"变更提案 + 状态" + 一个两阶段引擎(submit 产出挂起 / commit 原子落库) + 状态机(PENDING→APPROVED→EXECUTED)**。

- "变更清单不立即写库" = `submit` 只 `buildEdits` 存 JSON,不碰业务库;
- "审批后真执行" = `approve` 触发 `commit`,`@Transactional` 里 `applyEdit` 才真正写库 + 调外部系统;
- 全程 `action_audit_log` 留痕。

机制借鉴 Palantir Ontology Action 的“对象语义 + 可执行动作 + 权限与审计”思想，但审批记录、执行步骤、事务和工作流是本系统自己的实现，不能表述成 Palantir 的固定内部架构。**不依赖 AI,也不依赖 Palantir,可自行实现。**

---

## 10. 当前实现口径（2026-09-06）

### 10.1 条件字段与触发对象

对象类型必须配置主属性，用于生成稳定 `objectKey`：

- 属性映射命中物理表主键时，系统自动将对应属性设为主属性；
- 物理表没有主键时，在“概念管理 → 设置主属性”中人工选择一个已映射且业务上唯一的属性；
- 联合主键选择多个属性，共同组成规范化 JSON `objectKey`；
- 未配置主属性的概念只能浏览，不能执行对象级修改、删除或动作。

人工执行对象绑定动作时，不再要求操作者手工输入 `objectKey` 或 WHERE 条件参数：

1. 在动作管理中通过简单属性过滤器查询对象；
2. 选择一条对象记录；
3. 系统根据概念主键生成稳定 `objectKey`；
4. 条件属性选择“订单编号”时，配置自动保存为 `${orderNo}`；条件操作符默认为“等于”，也可选择不等于、大于、大于等于、小于、小于等于、包含或属于列表；
5. 提交预览和 Worker 真正执行前，后端都根据 `objectKey` 重新读取 `object.orderNo`，不信任前端传入的条件值。

对象浏览器也提供“执行动作”入口：当前行就是触发对象，只展示绑定到该概念且允许人工触发的动作。

### 10.2 当前值运算不是普通占位符

`${stock}` 表示执行调用方提供的参数，不表示数据库中的当前库存。因此“库存减 1”不能配置成普通入参引用，应使用目标属性的“当前值运算”模式：

```text
目标属性：库存数量
取值模式：当前值运算
运算：当前值 -
运算量：1
```

系统生成的赋值语义为：

```sql
库存物理列 = 库存物理列 - 1
```

如果运算量来自执行参数，可以填写 `${quantity}`，表示：

```sql
库存物理列 = 库存物理列 - :quantity
```

后端只接受数字运算量，不允许通过该模式注入任意 SQL。

### 10.3 “发货”是一个业务动作，内部是两个原子步骤

“订单发货并扣减库存”对用户应呈现为一个动作，只进行一次人工确认；其内部至少包含：

1. 条件检查：订单状态为“待发货”，可用库存大于等于订单数量；
2. 原子步骤 A：按库存充足条件扣减库存；
3. 原子步骤 B：把订单状态更新为“已发货”；
4. 两步都成功才记录动作成功，任一步失败则整体回滚或进入补偿。

当前实现不再把 `conceptId` 同时当成唯一执行目标，而是拆成：

- `conceptId`：触发对象类型，人工执行时用它查询和选择具体对象；
- `executionSteps`：多目标动作的有序步骤，每步单独选择目标对象类型、操作类型、目标属性和定位条件；
- 每个步骤的定位条件由“目标属性 + 操作符 + 触发对象取值属性”组成；同名属性自动匹配，不同名时显式选择，例如“库存.productCode 等于 订单.sku”；
- 同一数据源的多个步骤：使用同一个数据库事务顺序执行，任一步失败则整体回滚；
- 跨数据源或外部系统：当前版本拒绝保存为本地原子动作，应使用工作流与补偿机制，不能伪装成数据库原子事务；
- 人工确认属于整个动作，一次动作最多确认一次，内部步骤不分别确认。

“发货”可配置为一个“多目标动作”：步骤 1 对库存对象执行“当前值 - `${quantity}`”，步骤 2 把订单状态改为“已发货”。前置检查在提交时和真正执行前各运行一次。

### 10.4 数据到达属于事件/工作流触发

技术上，数据到达事件只要能携带概念、稳定对象主键和事件 ID，就可以触发单个对象动作。但“订单数据到达”通常只表示订单被创建或更新，并不等于真实发货已经发生。

触发来源是执行记录的运行时属性，不是动作定义上的单选项。动作定义界面不再选择“人工/数据到达”，而只配置“直接执行/人工确认后执行”。因此推荐口径是：

- 人工触发：动作管理或对象浏览器选择具体对象后提交；
- 数据到达：由事件规则/工作流携带 `actionId`、对象标识、事件 ID 和 `triggerType=DATA_ARRIVAL` 提交动作；
- API、工作流和人工入口使用同一提交服务；
- 是否需要人工确认只取决于动作的执行模式。数据到达不会被强制改成直接执行；需要真实世界人工确认的动作仍会创建待确认任务；
- 无需人工确认的动作无论从哪个入口触发，都由 Worker 直接执行，但完整保留过程记录。

Palantir Ontology 中有 Action、Object、Property、Link 等原语，但没有独立的“Decision”原语。本系统中的“决策提案、条件检查、人工确认、工作流”是建立在 Action 和运行时记录之上的产品能力，不应描述成 Palantir 原生的 Decision 对象。
