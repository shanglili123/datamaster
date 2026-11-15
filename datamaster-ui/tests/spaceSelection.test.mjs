import test from 'node:test'
import assert from 'node:assert/strict'
import { selectAccessibleSpace } from '../src/utils/spaceSelection.js'
const spaces = [{ id: '3', code: 'space3', name: '第三空间' }, { id: '1', code: 'space1', name: '第一空间' }]
test('initial login selects the first accessible space in server order', () => {
  assert.equal(selectAccessibleSpace(spaces, null), spaces[0])
})
test('retains a still accessible previous selection across string and numeric ids', () => {
  assert.equal(selectAccessibleSpace(spaces, 1), spaces[1])
})
test('removed or inaccessible previous selection falls back to first accessible space', () => {
  assert.equal(selectAccessibleSpace(spaces, '99'), spaces[0])
})
test('disabled deleted and incomplete entries cannot become default space', () => {
  const invalid = [{ id: '9', code: 'x', validFlag: false }, { id: '8', code: 'x', delFlag: true }, { id: '7', code: 'x', validFlag: '0' }, { id: '6', code: 'x', delFlag: '1' }, { id: '5' }, null]
  assert.equal(selectAccessibleSpace([...invalid, ...spaces], '9'), spaces[0])
})
test('no accessible spaces stays unselected and malformed inventory is not an empty success', () => {
  assert.equal(selectAccessibleSpace([], '1'), null)
  assert.throws(() => selectAccessibleSpace(undefined, '1'), /空间列表响应无效/)
})
