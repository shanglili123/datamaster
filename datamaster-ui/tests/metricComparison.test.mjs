import test from 'node:test'
import assert from 'node:assert/strict'
import { metricComparisonBlockReason, metricCustomBaselineReason } from '../src/utils/metricComparison.js'

const query = () => ({ metricId: 10, version: 1, pageNum: 1, dimensionPropertyIds: [2], timeBucket: 'NONE', sortBy: null, startTime: '2026-01-01', endTime: '2026-02-01' })
test('only first page default or chronological grouped requests are offered', () => {
  const request = query()
  assert.equal(metricComparisonBlockReason(request), '')
  request.pageNum = 2; assert.match(metricComparisonBlockReason(request), /第一页/)
  request.pageNum = 1; request.sortBy = 'metric_value'; request.sortDirection = 'DESC'
  assert.match(metricComparisonBlockReason(request), /TopN/)
  request.timeBucket = 'MONTH'; request.sortBy = 'time_bucket'; request.sortDirection = 'ASC'
  assert.equal(metricComparisonBlockReason(request), '')
  request.sortDirection = 'DESC'; assert.match(metricComparisonBlockReason(request), /TopN/)
})
test('period comparison needs an explicit current range but multi totals can omit time', () => {
  const request = query(); request.startTime = null; request.endTime = null; request.dimensionPropertyIds = []
  assert.match(metricComparisonBlockReason(request, { period: true }), /开始和结束/)
  assert.equal(metricComparisonBlockReason(request), '')
})
test('period total sort compatibility does not relax grouped or multi requests', () => {
  const request = query(); request.dimensionPropertyIds = []; request.sortBy = 'metric_value'; request.sortDirection = 'DESC'
  assert.equal(metricComparisonBlockReason(request, { period: true }), '')
  assert.match(metricComparisonBlockReason(request), /TopN/)
  request.dimensionPropertyIds = [2]; assert.match(metricComparisonBlockReason(request, { period: true }), /TopN/)
})
test('custom baseline rejects invalid calendars, reversed ranges and mismatched precision kinds', () => {
  assert.equal(metricCustomBaselineReason(query(), '2024-02-01', '2024-03-01'), '')
  assert.match(metricCustomBaselineReason(query(), '2026-02-30', '2026-03-01'), /无效/)
  assert.match(metricCustomBaselineReason(query(), '2026-03-01', '2026-02-01'), /无效/)
  assert.match(metricCustomBaselineReason(query(), '2026-01-01T00:00', '2026-02-01T00:00'), /无效/)
  assert.match(metricCustomBaselineReason(query(), '', ''), /补齐/)
  const timed = { ...query(), startTime: '2026-01-01T00:00' }
  assert.equal(metricCustomBaselineReason(timed, '2025-01-01T00:00', '2025-02-01T00:00:00.000000001'), '')
  assert.match(metricCustomBaselineReason(timed, '2025-01-01', '2025-02-01'), /无效/)
})

test('incomplete main query provides an explanation rather than a silent disabled control', () => {
  assert.match(metricComparisonBlockReason(null), /补齐/)
  assert.match(metricComparisonBlockReason({ metricId: 10 }), /发布版本/)
})
