import test from 'node:test'
import assert from 'node:assert/strict'
import { metricQuickTimeRange } from '../src/utils/metricTimeRange.js'

// Source-only regressions using Node's built-in runner; no browser, network or dependencies.
test('positive business offset crosses UTC year boundary without browser conversion', () => {
  assert.deepEqual(metricQuickTimeRange('TODAY', '+14:00', false, new Date('2025-12-31T12:30:00Z')), {
    startTime: '2026-01-01T00:00:00', endTime: '2026-01-02T00:00:00'
  })
})
test('negative business offset retains previous calendar date', () => {
  assert.deepEqual(metricQuickTimeRange('TODAY', '-12:00', true, new Date('2026-01-01T05:00:00Z')), {
    startTime: '2025-12-31', endTime: '2026-01-01'
  })
})
test('last full month preserves leap day and exclusive boundary', () => {
  assert.deepEqual(metricQuickTimeRange('LAST_MONTH', '+00:00', true, new Date('2024-03-15T12:00:00Z')), {
    startTime: '2024-02-01', endTime: '2024-03-01'
  })
})
test('seven calendar days include today rather than subtracting seven days', () => {
  assert.deepEqual(metricQuickTimeRange('LAST_7_DAYS', '+08:00', true, new Date('2026-01-02T18:00:00Z')), {
    startTime: '2025-12-28', endTime: '2026-01-04'
  })
})
test('this month ends after the current business day', () => {
  assert.deepEqual(metricQuickTimeRange('THIS_MONTH', '+08:00', true, new Date('2026-11-11T18:00:00Z')), {
    startTime: '2026-11-01', endTime: '2026-11-13'
  })
})
test('undeclared offset uses browser date labels for today only', () => {
  const now = new Date(2026, 10, 12, 12)
  assert.deepEqual(metricQuickTimeRange('TODAY', null, true, now), {
    startTime: '2026-11-12', endTime: '2026-11-13'
  })
})
test('invalid publication offsets and unsupported years are rejected', () => {
  for (const offset of ['Asia/Shanghai', '-00:00', '+14:01', '-15:00', '+00:60']) {
    assert.throws(() => metricQuickTimeRange('TODAY', offset, true, new Date('2026-11-12T12:00:00Z')))
  }
  assert.throws(() => metricQuickTimeRange('UNKNOWN', '+00:00', true))
  assert.throws(() => metricQuickTimeRange('TODAY', '+00:00', true, new Date('invalid')))
  assert.throws(() => metricQuickTimeRange('TODAY', '+00:00', true, new Date('9999-12-31T12:00:00Z')))
})
