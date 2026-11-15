// Business-local ISO labels only: no Date parsing, browser timezone or implicit normalization.
export function metricTemporalValue(value, dataType) {
  const type = typeof dataType === 'string' ? dataType.toLowerCase() : ''
  if (!['date', 'datetime', 'timestamp'].includes(type) || typeof value !== 'string') throw new Error('日期属性需要明确类型及ISO字符串')
  const pattern = type === 'date' ? /^(\d{4})-(\d{2})-(\d{2})$/ : /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/
  const match = pattern.exec(value)
  if (!match || match[0] !== value) throw new Error('日期须为YYYY-MM-DD，时间须为ISO本地时间，不接受空格或时区后缀')
  const year = Number(match[1]), month = Number(match[2]), day = Number(match[3])
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  if (year < 1 || year > 9999 || month < 1 || month > 12 || day < 1 || day > days[month - 1]) throw new Error('日期不存在或超出0001至9999年')
  if (type !== 'date' && (Number(match[4]) > 23 || Number(match[5]) > 59 || Number(match[6] || 0) > 59)) throw new Error('时间时分秒无效')
  return value
}

export function metricTemporalRange(start, end, dataType) {
  metricTemporalValue(start, dataType); metricTemporalValue(end, dataType)
  const comparable = value => {
    if (dataType.toLowerCase() === 'date') return value
    const full = value.length === 16 ? `${value}:00` : value
    const [seconds, fraction = ''] = full.split('.')
    return `${seconds}.${fraction.padEnd(9, '0')}`
  }
  if (comparable(start) >= comparable(end)) throw new Error('开始时间必须早于结束时间（结束不含）')
  return { startTime: start, endTime: end }
}
