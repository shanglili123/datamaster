import test from 'node:test'
import assert from 'node:assert/strict'
import { metricBucketRange, metricDrillFilters, nextMetricDrillDimension } from '../src/utils/metricDrilldown.js'
import { metricFilterScalar } from '../src/utils/metricFilter.js'

test('partial daily bucket narrows by intersection and keeps nanosecond bounds', () => {
  assert.deepEqual(metricBucketRange('2026-11-12', 'DAY', false, '2026-11-12T08:00:00.123456789', '2026-11-13T10:00:00'), {
    startTime: '2026-11-12T08:00:00.123456789', endTime: '2026-11-13T00:00:00.000000000'
  })
})
test('equal whole-second and fractional boundaries cannot expand original range', () => {
  assert.deepEqual(metricBucketRange('2026-11-12', 'DAY', false, '2026-11-12T00:00:00.000', '2026-11-12T12:00'), {
    startTime: '2026-11-12T00:00:00.000000000', endTime: '2026-11-12T12:00:00.000000000'
  })
})
test('date-only leap-month bucket respects original half-open range', () => {
  assert.deepEqual(metricBucketRange('2024-02', 'MONTH', true, '2024-02-10', '2024-03-10'), {
    startTime: '2024-02-10', endTime: '2024-03-01'
  })
})
test('invalid labels, nonintersecting buckets and overflow are rejected', () => {
  for (const [label, grain] of [['2026-02-30', 'DAY'], ['2026-02', 'DAY'], ['2026-02-01', 'MONTH'], ['2026-02', 'YEAR'], ['9999', 'YEAR']]) {
    assert.throws(() => metricBucketRange(label, grain, true, '2026-01-01', '2027-01-01'))
  }
  assert.throws(() => metricBucketRange('2026-11-12', 'DAY', true, '2026-11-13', '2026-11-14'))
  assert.throws(() => metricBucketRange('2026-11-12', 'DAY', false, '2026-11-12T24:00:00', '2026-11-13T00:00:00'))
})
test('empty text group survives drill, save serialization and filter submission', () => {
  const filters = metricDrillFilters([2, 3, 4], { d_2: '', d_3: null, d_4: 'null' })
  assert.deepEqual(filters, [
    { propertyId: 2, operator: 'EQ', value: '' },
    { propertyId: 3, operator: 'IS_NULL', value: '' },
    { propertyId: 4, operator: 'EQ', value: 'null' }
  ])
  const restored = JSON.parse(JSON.stringify(filters))
  assert.equal(metricFilterScalar(restored[0].value, 'string'), '')
  assert.equal(metricFilterScalar(restored[2].value, 'text'), 'null')
  assert.equal(nextMetricDrillDimension([2, 3, 4, 5], [], restored, null), 5)
})
test('missing or imprecise group values never become null filters', () => {
  for (const row of [{}, { d_2: undefined }, { d_2: {} }, { d_2: 9007199254740992 }]) assert.throws(() => metricDrillFilters([2], row))
})
test('next dimension excludes selected, fixed or user pinned, and time properties', () => {
  assert.equal(nextMetricDrillDimension([1, 2, 3, 4, 5], [1], [
    { propertyId: '2', operator: 'EQ' }, { propertyId: 3, operator: 'IS_NULL' }
  ], 4), 5)
  assert.equal(nextMetricDrillDimension([1, 2], [1], [{ propertyId: 2, operator: 'IS_NULL' }], null), undefined)
  assert.equal(nextMetricDrillDimension([1, 2], [1], [{ propertyId: 2, operator: 'IN' }], null), 2)
})
