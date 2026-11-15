// Validate the same bounded decimal syntax accepted by the backend BigDecimal path.
// Never convert the submitted text to Number: underflow/overflow is not decimal validity.
export function metricNumericFilterValue(value, dataType) {
  if (typeof value !== 'string' && typeof value !== 'number') throw new Error('数值属性需要字符串或数字')
  if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new Error('大整数及小数请使用带双引号的字符串，避免精度丢失')
  const text = String(value).trim()
  if (!text || text.length > 2000) throw new Error('数值过滤值为空或过长')
  const match = /^([+-]?)(?:(\d+)(?:\.(\d*))?|\.(\d+))(?:[eE]([+-]?\d+))?$/.exec(text)
  if (!match) throw new Error('数值属性需要有效十进制数字')
  const fraction = match[3] ?? match[4] ?? ''
  const digits = `${match[2] || ''}${fraction}`
  const significant = digits.replace(/^0+/, '')
  // Parse a bounded exponent lexically first, so huge exponent text never loses precision.
  const exponentText = match[5] || '0'
  const exponentDigits = exponentText.replace(/^[+-]/, '').replace(/^0+/, '') || '0'
  if (exponentDigits.length > 4) throw new Error('数值过滤值超出精度或scale上限')
  const exponent = Number(exponentText), scale = fraction.length - exponent
  if (Math.max(1, significant.length) > 1000 || Math.abs(scale) > 1000) throw new Error('数值过滤值超出精度或scale上限')
  const type = String(dataType).toLowerCase()
  if (['integer', 'long', 'int', 'bigint', 'smallint'].includes(type) && significant) {
    const trailingZeros = /0*$/.exec(significant)[0].length
    if (scale > trailingZeros) throw new Error('整数属性不接受小数')
  }
  return text
}
