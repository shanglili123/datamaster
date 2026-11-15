import { metricTemporalRange } from './metricTemporal.js'

export function metricCustomBaselineReason(query, start, end) {
  if (!start || !end) return '请补齐自定义基期的开始和结束时间'
  const type = query?.startTime?.length === 10 ? 'date' : 'datetime'
  try { metricTemporalRange(start, end, type); return '' }
  catch (failure) { return `自定义基期无效：${failure.message}` }
}

// UI guidance only. Source/column permissions and actual completeness remain backend checks.
export function metricComparisonBlockReason(query, { period = false } = {}) {
  if (!query?.metricId || !query.version) return '请先在上方补齐有效的指标、发布版本及查询条件'
  if ((query.pageNum ?? 1) !== 1) return '比较须从第一页核验完整分组，请先返回第一页'
  if (period && (!query.startTime || !query.endTime)) return '两期比较须明确本期开始和结束时间'
  const bucket = query.timeBucket || 'NONE'
  const chronological = bucket !== 'NONE' && query.sortBy === 'time_bucket' && query.sortDirection === 'ASC'
  const total = !(query.dimensionPropertyIds || []).length && bucket === 'NONE'
  if (query.sortBy && !chronological && !(period && total)) return '比较不接受独立TopN排序，请改用默认顺序或时间桶升序'
  return ''
}
