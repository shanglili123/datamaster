import { metricTemporalValue } from './metricTemporal.js'

// Calendar labels are business-local labels, not browser Date instants.
export function metricBucketRange(label, grain, dateOnly, originalStart, originalEnd) {
  const pad = value => String(value).padStart(2, '0')
  const match = typeof label === 'string' && /^(\d{4})(?:-(\d{2}))?(?:-(\d{2}))?$/.exec(label)
  if (!match || !['DAY', 'MONTH', 'YEAR'].includes(grain)) throw new Error('时间桶标签无效')
  const year = Number(match[1]), month = Number(match[2] || 1), day = Number(match[3] || 1)
  if (year < 1 || year > 9999 || month < 1 || month > 12 || day < 1 || day > 31
    || grain === 'DAY' && !match[3] || grain === 'MONTH' && (!match[2] || match[3]) || grain === 'YEAR' && match[2]) throw new Error('时间桶粒度与标签不一致')
  // UTC is used only for Gregorian calendar arithmetic; no timezone conversion is performed.
  const start = new Date(0); start.setUTCFullYear(year, month - 1, day); start.setUTCHours(0, 0, 0, 0)
  if (start.getUTCFullYear() !== year || start.getUTCMonth() !== month - 1 || start.getUTCDate() !== day) throw new Error('时间桶日期不存在')
  const end = new Date(start)
  if (grain === 'DAY') end.setUTCDate(end.getUTCDate() + 1)
  else if (grain === 'MONTH') end.setUTCMonth(end.getUTCMonth() + 1)
  else end.setUTCFullYear(end.getUTCFullYear() + 1)
  if (end.getUTCFullYear() > 9999) throw new Error('桶结束时间超出支持范围')
  const format = value => `${String(value.getUTCFullYear()).padStart(4, '0')}-${pad(value.getUTCMonth() + 1)}-${pad(value.getUTCDate())}${dateOnly ? '' : 'T00:00:00'}`
  const normalize = value => {
    metricTemporalValue(value, dateOnly ? 'date' : 'datetime')
    if (dateOnly) return value
    const full = value.length === 16 ? `${value}:00` : value
    const [seconds, fraction = ''] = full.split('.')
    return `${seconds}.${fraction.padEnd(9, '0')}`
  }
  const before = normalize(originalStart), after = normalize(originalEnd)
  const bucketStart = normalize(format(start)), bucketEnd = normalize(format(end))
  const narrowedStart = before > bucketStart ? before : bucketStart
  const narrowedEnd = after < bucketEnd ? after : bucketEnd
  if (narrowedStart >= narrowedEnd) throw new Error('该桶不在当前查询范围内')
  return { startTime: narrowedStart, endTime: narrowedEnd }
}

// These are editor values; SQL null is represented by IS_NULL, never by a guessed string.
export function metricDrillFilters(selected, row) {
  if (!row || typeof row !== 'object' || Array.isArray(row)) throw new Error('分组结果行无效')
  return selected.map(id => {
    const key = `d_${id}`
    if (!Object.prototype.hasOwnProperty.call(row, key)) throw new Error('分组键缺失，不能构造下钻条件')
    const value = row[key]
    if (value === undefined || value !== null && !['string', 'number', 'boolean'].includes(typeof value)) throw new Error('分组值无效，不能构造下钻条件')
    if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new Error('分组数值精度不可确认，不能构造下钻条件')
    return { propertyId: id, operator: value === null ? 'IS_NULL' : 'EQ', value: value === null ? '' : String(value) }
  })
}

export function nextMetricDrillDimension(published, selected, filters, timePropertyId) {
  const pinned = new Set((filters || []).filter(filter => ['EQ', 'IS_NULL'].includes(filter.operator)).map(filter => String(filter.propertyId)))
  for (const id of selected || []) pinned.add(String(id))
  return (published || []).find(id => !pinned.has(String(id)) && String(id) !== String(timePropertyId))
}
