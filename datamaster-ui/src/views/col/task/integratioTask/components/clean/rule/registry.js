import { defineAsyncComponent } from "vue"
const modules = import.meta.glob("./**/*.vue")

/**
 * 清洗规则注册清单：仅维护元信息与组件路径
 * 组件解析通过 import.meta.glob + defineAsyncComponent 按需加载
 */
export const ruleRegistry = {
  "001": { label: "数值边界值调整", componentPath: "./numberBoundaryRule.vue" },
  "008": { label: "小数位数", componentPath: "./decimalFormatterRule.vue" },
  "010": { label: "字段前缀/后缀统一", componentPath: "./affixEditorRule.vue" },
  "024": { label: "枚举值映射标准化", componentPath: "./enumMapRule/index.vue" },
  "029": { label: "按组合字段去重", componentPath: "./combinerFieldUniqueRule.vue" },
  "011": { label: "正则表达式替换", componentPath: "./regexReplaceRule.vue" },
  "039": { label: "清理过期记录", componentPath: "./cleanExpiredDataRule.vue" },
  "012": { label: "超长字段截断", componentPath: "./longFieldTruncator.vue" },
  "019": { label: "组合字段为空删除", componentPath: "./emptyRule.vue" },
  "022": { label: "字段转小写", componentPath: "./emptyRule.vue" },
  "007": { label: "日期格式统一", componentPath: "./dateFormatter.vue" },
  "009": { label: "去除字段空格", componentPath: "./trimSpaceRule.vue" },
  EMPTY: { label: "占位规则", componentPath: "./emptyRule.vue" },


}

/**
 * 获取规则元信息
 * @param {string} key 规则编码（ruleCode）
 * @returns {object|null}
 */
export function getRuleConfig(key) {
  return ruleRegistry[key] || null
}

/**
 * 获取规则异步组件
 * @param {string} key 规则编码（ruleCode）
 * @returns {import('vue').DefineComponent|null}
 */
export function getRuleComponent(key) {
  const cfg = getRuleConfig(key)
  const path = cfg?.componentPath
  if (!path || !modules[path]) return null
  return defineAsyncComponent(() => modules[path]())
}
