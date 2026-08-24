// 编码自动生成：前缀_时间戳36进制+4位随机，免人工输入且唯一
export function genCode(prefix) {
  const ts = Date.now().toString(36)
  const rand = Math.random().toString(36).slice(2, 6)
  return `${prefix}_${ts}${rand}`
}
