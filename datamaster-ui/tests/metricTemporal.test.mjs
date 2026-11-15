import test from 'node:test'
import assert from 'node:assert/strict'
import { metricTemporalValue, metricTemporalRange } from '../src/utils/metricTemporal.js'
import { metricFilterScalar } from '../src/utils/metricFilter.js'

test('Gregorian leap dates and bounded years are validated without browser timezone', () => {
  for (const value of ['0001-01-01', '2000-02-29', '2024-02-29', '9999-12-31']) assert.equal(metricTemporalValue(value, 'date'), value)
  assert.throws(() => metricTemporalValue('2026-11-12\n', 'date'))
  assert.throws(() => metricTemporalValue('2026-11-12T10:30\n', 'datetime'))
  for (const value of ['0000-01-01', '1900-02-29', '2026-02-29', '2026-04-31', '2026-00-01', '2026-01-00']) assert.throws(() => metricTemporalValue(value, 'date'))
})
test('local timestamps preserve nanosecond text and reject offsets or normalization', () => {
  for (const value of ['2026-11-12T10:30', '2026-11-12T10:30:00', '2026-11-12T10:30:00.123456789']) assert.equal(metricTemporalValue(value, 'timestamp'), value)
  for (const value of ['2026-11-12 10:30:00', '2026-11-12T24:00', '2026-11-12T10:60', '2026-11-12T10:30:60', '2026-11-12T10:30Z', '2026-11-12T10:30:00+08:00', '2026-11-12T10:30:00.1234567890']) assert.throws(() => metricTemporalValue(value, 'datetime'))
})
test('half-open ranges compare normalized fractions without changing input', () => {
  const start = '2026-11-12T10:30', end = '2026-11-12T10:30:00.000000001'
  assert.deepEqual(metricTemporalRange(start, end, 'datetime'), { startTime: start, endTime: end })
  assert.throws(() => metricTemporalRange(start, '2026-11-12T10:30:00.000', 'timestamp'))
  assert.throws(() => metricTemporalRange('2026-11-13', '2026-11-12', 'date'))
  assert.throws(() => metricTemporalRange('2026-11-12', '2026-11-13T00:00', 'date'))
})
test('management and analysis filters use the same temporal validation', () => {
  assert.equal(metricFilterScalar('2024-02-29', 'DATE'), '2024-02-29')
  assert.throws(() => metricFilterScalar('2026-02-30', 'date'))
  assert.throws(() => metricFilterScalar('2026-11-12 10:30:00', 'timestamp'))
  assert.equal(metricFilterScalar('', 'string'), '')
})
