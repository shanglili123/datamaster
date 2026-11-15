import test from 'node:test'
import assert from 'node:assert/strict'
import { loadPagedOptions } from '../src/api/ont/metricOptions.js'

const page = (start, count) => Array.from({ length: count }, (_, index) => ({ id: start + index }))
test('paginates with page size 100 and supports numeric string totals', async () => {
  const calls = []
  const rows = await loadPagedOptions(async params => {
    calls.push(params)
    return { data: { rows: params.pageNum === 1 ? page(1, 100) : page(101, 1), total: '101' } }
  }, { ontologyId: 20 })
  assert.equal(rows.length, 101)
  assert.deepEqual(calls.map(item => [item.pageNum, item.pageSize, item.ontologyId]), [[1, 100, 20], [2, 100, 20]])
})
test('cancelled context discards pending page and stops subsequent requests', async () => {
  let current = true, calls = 0
  const rows = await loadPagedOptions(async () => {
    calls += 1; current = false
    return { data: { rows: page(1, 100), total: 200 } }
  }, {}, { isCurrent: () => current })
  assert.deepEqual(rows, []); assert.equal(calls, 1)
})
test('already invalid context never sends the first request', async () => {
  let called = false
  assert.deepEqual(await loadPagedOptions(async () => { called = true }, {}, { isCurrent: () => false }), [])
  assert.equal(called, false)
})
test('repeated pages, malformed responses and premature endings never return partial options', async () => {
  await assert.rejects(loadPagedOptions(async () => ({ data: { rows: page(1, 100), total: 200 } })), /重复/)
  await assert.rejects(loadPagedOptions(async () => ({ data: { rows: page(1, 2), total: 100 } })), /不完整/)
  await assert.rejects(loadPagedOptions(async () => ({ data: {} })), /响应无效/)
  await assert.rejects(loadPagedOptions(async () => ({ data: { rows: [{}], total: 1 } })), /标识/)
  await assert.rejects(loadPagedOptions(async () => ({ data: { rows: [], total: -1 } })), /总数/)
  await assert.rejects(loadPagedOptions(async () => ({ data: { rows: page(1, 2), total: 1 } })), /总数已变化/)
  await assert.rejects(loadPagedOptions(async () => ({ data: { rows: page(1, 101) } })), /响应无效/)
})
test('bounded loader reports limit rather than silently truncating', async () => {
  let calls = 0
  await assert.rejects(loadPagedOptions(async () => {
    calls += 1; return { data: { rows: page((calls - 1) * 100 + 1, 100), total: 1000 } }
  }, {}, { maxPages: 2 }), /上限/)
  assert.equal(calls, 2)
})
test('declared zero total is a valid empty inventory', async () => {
  assert.deepEqual(await loadPagedOptions(async () => ({ data: { rows: [], total: 0 } })), [])
})
test('unknown total ends on a short page including legitimate empty inventory', async () => {
  assert.deepEqual(await loadPagedOptions(async () => ({ data: { rows: [] } })), [])
  const rows = await loadPagedOptions(async () => ({ data: { rows: page(1, 2) } }))
  assert.equal(rows.length, 2)
})
