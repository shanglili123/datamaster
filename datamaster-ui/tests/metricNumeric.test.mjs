import test from 'node:test'
import assert from 'node:assert/strict'
import { metricNumericFilterValue } from '../src/utils/metricNumeric.js'

test('exact decimal strings retain values beyond JavaScript number range', () => {
  for (const value of ['9007199254740993', '12345678901234567890.123456789', '1e400', '1e-400', '.50', '+1.', '-0.000']) {
    assert.equal(metricNumericFilterValue(value, 'decimal'), value)
  }
  assert.equal(metricNumericFilterValue(' 12.50 ', 'decimal'), '12.50')
})
test('numeric JSON values must not arrive with unsafe integer or decimal precision', () => {
  assert.equal(metricNumericFilterValue(42, 'integer'), '42')
  for (const value of [9007199254740992, 1.25, Infinity, NaN, true, null, [], {}]) {
    assert.throws(() => metricNumericFilterValue(value, 'decimal'))
  }
})
test('decimal syntax excludes JavaScript-only representations and malformed input', () => {
  for (const value of ['', ' ', '0x10', '0b10', 'Infinity', 'NaN', '1_000', '1,000', '.', '1e', '++1', '1 2']) {
    assert.throws(() => metricNumericFilterValue(value, 'decimal'))
  }
})
test('precision and scale limits mirror the bounded BigDecimal contract', () => {
  assert.equal(metricNumericFilterValue('1e1000', 'decimal'), '1e1000')
  assert.equal(metricNumericFilterValue('1e-1000', 'decimal'), '1e-1000')
  assert.equal(metricNumericFilterValue('0'.repeat(100) + '1', 'decimal'), '0'.repeat(100) + '1')
  for (const value of ['1e1001', '1e-1001', '1e9999999999999999999', '1'.repeat(1001), '0'.repeat(2001)]) {
    assert.throws(() => metricNumericFilterValue(value, 'decimal'))
  }
})
test('integer types permit exact scientific integers but reject fractional values', () => {
  for (const value of ['1.00', '100e-2', '1.20e1', '0.000', '-20', '1e400']) {
    assert.equal(metricNumericFilterValue(value, 'bigint'), value)
  }
  for (const value of ['1.20', '1e-1', '100e-3', '0.001']) {
    assert.throws(() => metricNumericFilterValue(value, 'integer'))
  }
})
