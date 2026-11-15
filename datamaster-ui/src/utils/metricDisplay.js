// Presentation only. Never write these labels into a query, result row or saved definition.
export function metricDisplayValue(value, missing = '—') {
  if (value == null) return missing
  if (value === '') return '空字符串（""）'
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}
