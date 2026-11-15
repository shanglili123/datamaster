import { metricNumericFilterValue } from './metricNumeric.js'
import { metricTemporalValue } from './metricTemporal.js'

// Text '' is an actual dimension value, distinct from missing input and SQL null.
export function metricFilterScalar(value, dataType) {
  if (value == null || !['string', 'number', 'boolean'].includes(typeof value)) throw new Error('过滤值必须为非空标量，SQL空值请使用IS_NULL')
  const type = dataType?.toLowerCase()
  if (['integer', 'decimal', 'number', 'long', 'int', 'double', 'float', 'numeric', 'bigint', 'smallint'].includes(type)) {
    return metricNumericFilterValue(value, type)
  }
  if (type === 'boolean') {
    if (![true, false, 'true', 'false'].includes(value)) throw new Error('布尔值只能为 true 或 false')
    return value === true || value === 'true'
  }
  if (!['string', 'text', 'varchar', 'char', 'date', 'datetime', 'timestamp'].includes(type)) throw new Error('过滤属性类型缺失或不支持，请重新加载版本')
  if (typeof value !== 'string' || value.length > 2000) throw new Error('文本及日期过滤值须为最多2000字符的字符串')
  if (['date', 'datetime', 'timestamp'].includes(type)) return metricTemporalValue(value, type)
  return value
}
