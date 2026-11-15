// Business dates are calendar labels, not instants in the browser's timezone.
// With no declared offset, only today's date uses the browser's local calendar.
export function metricQuickTimeRange(mode, businessOffset, dateOnly, now = new Date()) {
  if (!['TODAY', 'LAST_7_DAYS', 'THIS_MONTH', 'LAST_MONTH'].includes(mode)) throw new Error('未知快捷时间范围')
  if (!(now instanceof Date) || !Number.isFinite(now.getTime())) throw new Error('当前时间无效')
  let year = now.getFullYear(), month = now.getMonth(), day = now.getDate()
  if (businessOffset != null && businessOffset !== '') {
    const match = /^([+-])(\d{2}):(\d{2})$/.exec(businessOffset)
    if (!match) throw new Error('发布的业务时间偏移格式无效')
    const hours = Number(match[2]), minutes = Number(match[3])
    if (hours > 14 || minutes > 59 || hours === 14 && minutes !== 0 || businessOffset === '-00:00') throw new Error('发布的业务时间偏移超出范围')
    const offset = (match[1] === '-' ? -1 : 1) * (hours * 60 + minutes)
    const business = new Date(now.getTime() + offset * 60000)
    year = business.getUTCFullYear(); month = business.getUTCMonth(); day = business.getUTCDate()
  }
  // UTC is only a Gregorian arithmetic carrier. Do not reconstruct these labels
  // using local Date constructors: a browser may skip a local midnight/day.
  const start = new Date(0); start.setUTCFullYear(year, month, day); start.setUTCHours(0, 0, 0, 0)
  const end = new Date(start); end.setUTCDate(end.getUTCDate() + 1)
  if (mode === 'LAST_7_DAYS') start.setUTCDate(start.getUTCDate() - 6)
  if (mode === 'THIS_MONTH') start.setUTCDate(1)
  if (mode === 'LAST_MONTH') {
    start.setUTCDate(1); end.setTime(start.getTime()); start.setUTCMonth(start.getUTCMonth() - 1)
  }
  const format = value => {
    const y = value.getUTCFullYear()
    if (!Number.isInteger(y) || y < 1 || y > 9999) throw new Error('快捷时间范围超出支持年份')
    return `${String(y).padStart(4, '0')}-${String(value.getUTCMonth() + 1).padStart(2, '0')}-${String(value.getUTCDate()).padStart(2, '0')}${dateOnly ? '' : 'T00:00:00'}`
  }
  return { startTime: format(start), endTime: format(end) }
}
