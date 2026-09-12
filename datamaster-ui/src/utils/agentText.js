/**
 * 将底层模型/适配器名称转换为平台对外的产品名称。
 * DB-GPT 只是决策智能体使用的适配器，不能出现在用户可见文本中。
 */
export function normalizeAgentText(value) {
  if (value === undefined || value === null) return value
  return String(value)
    .replace(/DB[-_ ]?GPT/gi, '决策智能体')
    .replace(/AI问数/g, '决策智能体')
    // Skill 是内部上下文，模型偶尔会把文档追加到回答末尾，前端也要做一次兜底清理。
    .replace(/(?:^|\n)[ \t]*Skill\s*:\s*[^\r\n]*(?:[\r\n]+[\s\S]*)?$/i, '')
    // 不把 react-agent 的内部工具轨迹、容器目录列表展示给用户。
    .replace(/^\s*\[[^\]\r\n]+\]\s*(?=(思考|动作|原因|输入)\s*[:：])/gim, '')
    .replace(/^\s*(思考|动作|原因|输入)\s*[:：].*$/gim, '')
    .replace(/^\s*[bcdlps-][rwx-]{9}\.?\s+\d+\s+\S+\s+\S+\s+\d+\s+.*$/gim, '')
    .replace(/^\s*.*\bop_snapshots\b.*$/gim, '')
}
