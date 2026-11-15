import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = await readFile(new URL('../src/utils/decisionConfirmation.js', import.meta.url), 'utf8')
const isolated = source.replace("import { Modal } from 'ant-design-vue'", 'const Modal = globalThis.__decisionConfirmationModal')
let dialog
// Only the UI dialog is replaced; no API request or business operation is run.
globalThis.__decisionConfirmationModal = { confirm(options) { dialog = options } }
const { confirmDecisionInput } = await import(`data:text/javascript;base64,${Buffer.from(isolated).toString('base64')}`)
delete globalThis.__decisionConfirmationModal

test('confirmation stays pending until the user explicitly accepts', async () => {
  let accepted = false
  const result = confirmDecisionInput('请核对输入', '明确确认', { confirmButtonText: '提交', cancelButtonText: '返回' })
  result.then(() => { accepted = true })
  await Promise.resolve()
  assert.equal(accepted, false)
  assert.equal(dialog.title, '明确确认')
  assert.equal(dialog.content, '请核对输入')
  assert.equal(dialog.okText, '提交')
  assert.equal(dialog.cancelText, '返回')
  assert.equal(dialog.maskClosable, false)
  dialog.onOk()
  await result
  assert.equal(accepted, true)
})

test('cancel rejects with the existing cancellation sentinel', async () => {
  const result = confirmDecisionInput('不提交', '确认')
  const rejection = assert.rejects(result, reason => reason === 'cancel')
  assert.equal(dialog.okText, '确认')
  assert.equal(dialog.cancelText, '取消')
  dialog.onCancel()
  await rejection
})
