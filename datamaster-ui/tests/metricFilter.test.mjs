import test from 'node:test'
import assert from 'node:assert/strict'
import { metricFilterScalar } from '../src/utils/metricFilter.js'

test('empty string, literal null and SQL null have different meanings', () => {
  assert.equal(metricFilterScalar('', 'string'), '')
  assert.equal(metricFilterScalar('null', 'varchar'), 'null')
  assert.equal(metricFilterScalar(' ', 'text'), ' ')
  assert.throws(() => metricFilterScalar(null, 'string'))
  assert.throws(() => metricFilterScalar(undefined, 'string'))
})
test('numeric and boolean values follow the published type without text coercion', () => {
  assert.equal(metricFilterScalar('1e400', 'NUMERIC'), '1e400')
  assert.equal(metricFilterScalar('false', 'boolean'), false)
  assert.equal(metricFilterScalar(true, 'BOOLEAN'), true)
  assert.throws(() => metricFilterScalar('', 'decimal'))
  assert.throws(() => metricFilterScalar('', 'boolean'))
  assert.throws(() => metricFilterScalar('TRUE', 'boolean'))
  assert.throws(() => metricFilterScalar(42, 'string'))
  assert.throws(() => metricFilterScalar(false, 'varchar'))
})
test('blank temporal values, unknown types and nested values cannot be submitted', () => {
  for (const type of ['date', 'datetime', 'timestamp']) assert.throws(() => metricFilterScalar('', type))
  for (const type of [undefined, 'object', 'sql']) assert.throws(() => metricFilterScalar('x', type))
  for (const value of [[], {}, () => 'x']) assert.throws(() => metricFilterScalar(value, 'string'))
  assert.throws(() => metricFilterScalar('x'.repeat(2001), 'text'))
})
