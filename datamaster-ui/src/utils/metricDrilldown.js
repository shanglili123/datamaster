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
    if (typeof value !== 'string' || !(dateOnly ? /^\d{4}-\d{2}-\d{2}$/ : /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d{1,9})?)?$/).test(value)) throw new Error('原时间范围不是有效业务时间格式')
    const y = Number(value.slice(0, 4)), m = Number(value.slice(5, 7)), d = Number(value.slice(8, 10))
    const check = new Date(0); check.setUTCFullYear(y, m - 1, d); check.setUTCHours(0, 0, 0, 0)
    if (y < 1 || y > 9999 || check.getUTCFullYear() !== y || check.getUTCMonth() !== m - 1 || check.getUTCDate() !== d) throw new Error('原时间范围日期不存在')
    if (!dateOnly && (Number(value.slice(11, 13)) > 23 || Number(value.slice(14, 16)) > 59 || Number(value.slice(17, 19) || 0) > 59)) throw new Error('原时间范围时刻不存在')
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

export function nextMetricDrillDimension(published, selected, filters, timePropertyId) {
  const pinned = new Set((filters || []).filter(filter => ['EQ', 'IS_NULL'].includes(filter.operator)).map(filter => String(filter.propertyId)))
  for (const id of selected || []) pinned.add(String(id))
  return (published || []).find(id => !pinned.has(String(id)) && String(id) !== String(timePropertyId))
}
