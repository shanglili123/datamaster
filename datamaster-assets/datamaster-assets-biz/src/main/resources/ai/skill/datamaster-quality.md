---
name: datamaster-quality
description: DataMaster built-in quality capability used to enrich table ask-data skills.
---

# DataMaster 质量核检能力

## 能力边界

- 质量信息用于提醒问数结果可信度，不直接修改 SQL 结果。
- 若缺少质量报告，表级 Skill 必须提示“暂未找到近期质量核检记录”。
- 若发现质量异常，表级 Skill 必须说明异常类型、影响字段和建议确认动作。

## 风险表达

- 最近核检失败：提示数据可能不完整或不可信。
- 缺少核检记录：提示结果仅基于当前可查数据，不代表质量已验证。
- 存在空值、唯一性、枚举、范围、时间顺序等异常：说明影响统计口径。

## 表级 Skill 生成规则

- 不编造通过率、异常数量、核检时间。
- 没有自动接入质量日志时，明确标记为待接入。
- 涉及质量风险的回答应避免绝对化业务结论。
