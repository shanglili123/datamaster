import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = await readFile(new URL('../src/views/ai/chat/index/index.vue', import.meta.url), 'utf8')
const match = source.replace(/\r\n/g, '\n').match(/function finishSteps\(message\) \{[\s\S]*?\n\}\n/)
assert.ok(match, 'finishSteps must exist')
const finishSteps = new Function(`${match[0]}; return finishSteps`)()

for (const status of ['FAILED', 'NEED_INPUT', 'NEED_CONFIRMATION', 'CANCELLED', 'PARTIAL', undefined]) {
  test(`${status} must not turn source/query steps green`, () => {
    const message = { askResultStatus: status, steps: [{ status: 'done' }, { status: 'active' }, { status: 'pending' }] }
    finishSteps(message)
    assert.ok(message.steps.every(step => step.status !== 'done'))
  })
}
test('successful governed result completes steps', () => {
  const message = { askResultStatus: 'SUCCEEDED', steps: [{ status: 'active' }, { status: 'pending' }] }
  finishSteps(message)
  assert.ok(message.steps.every(step => step.status === 'done'))
})
