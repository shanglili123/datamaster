import test from 'node:test'
import assert from 'node:assert/strict'
import { metricDisplayValue } from '../src/utils/metricDisplay.js'

test('empty text does not look like SQL null, zero or false', () => {
  assert.equal(metricDisplayValue(''), '空字符串（""）')
  assert.equal(metricDisplayValue(null), '—')
  assert.equal(metricDisplayValue(undefined, '缺失'), '缺失')
  assert.equal(metricDisplayValue('null'), 'null')
  assert.equal(metricDisplayValue(0), '0')
  assert.equal(metricDisplayValue(false), 'false')
  assert.equal(metricDisplayValue('9007199254740993.123456789'), '9007199254740993.123456789')
})
test('display labels never replace underlying values', () => {
  const row = { d_2: '', metric_value: null }
  metricDisplayValue(row.d_2); metricDisplayValue(row.metric_value)
  assert.deepEqual(row, { d_2: '', metric_value: null })
})
