// Shared schema helpers for transform editors. Keep legacy type values in task parameters.
export const resultTypes = ['string', 'int', 'bigint', 'double', 'decimal', 'boolean', 'date', 'timestamp'];

export function isDecimal(type) {
  return /^(decimal|numeric|bignumber)(\s*\(|$)/i.test(type || '');
}

export function validateDecimal(schema, type) {
  if (!isDecimal(type)) return '';
  if (!Number.isInteger(schema.precision) || schema.precision < 1 || schema.precision > 38) {
    return 'DECIMAL 总位数必须为 1 至 38 的整数';
  }
  if (!Number.isInteger(schema.scale) || schema.scale < 0 || schema.scale > schema.precision) {
    return 'DECIMAL 小数位必须为 0 至总位数之间的整数';
  }
  return '';
}

export function fieldSchema(field, type, schema = {}) {
  if (!type) return { ...field };
  const result = { ...field, type, columnType: type };
  if (isDecimal(type)) {
    if (schema.precision != null) result.precision = schema.precision;
    if (schema.scale != null) result.scale = schema.scale;
  } else {
    delete result.precision;
    delete result.scale;
  }
  if (schema.nullable != null) result.nullable = schema.nullable;
  return result;
}

export function withOutputField(fields, output) {
  const exists = fields.some(field => field.columnName === output.columnName);
  return exists
    ? fields.map(field => field.columnName === output.columnName ? { ...field, ...output } : { ...field })
    : [...fields.map(field => ({ ...field })), output];
}
