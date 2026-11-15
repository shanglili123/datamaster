import test from 'node:test'
import assert from 'node:assert/strict'
import { modelRoutingPayload } from '../src/utils/modelRouting.js'
const scope = { spaceId: '10', spaceCode: 'space10' }
const profiles = [{ id: '9007199254740993', enabled: true, hasApiKey: true }, { id: '2', enabled: true, hasApiKey: true }]
const form = () => ({ enabled: true, economyProfileId: '9007199254740993', advancedProfileId: '2', semanticDecisionEnabled: false, strictJsonSchema: false, complexInputCharacters: 12000, revision: 0 })
test('preserves exact string ids and only emits explicit routing fields', () => {
  const result = modelRoutingPayload({ ...form(), apiKey: 'never-send', apiUrl: 'never-send' }, profiles, scope)
  assert.equal(result.economyProfileId, '9007199254740993')
  assert.equal(result.decisionProfileId, null)
  assert.equal(result.spaceId, '10')
  assert.equal('apiKey' in result, false)
  assert.equal('apiUrl' in result, false)
})
test('disabled policy can be saved without models to override deployment defaults', () => {
  assert.equal(modelRoutingPayload({ ...form(), enabled: false, economyProfileId: null, advancedProfileId: null }, [], scope).enabled, false)
})
test('enabled routing requires both enabled credentialed models', () => {
  assert.throws(() => modelRoutingPayload({ ...form(), advancedProfileId: null }, profiles, scope))
  assert.throws(() => modelRoutingPayload(form(), [{ ...profiles[0], enabled: false }, profiles[1]], scope))
  assert.throws(() => modelRoutingPayload(form(), [{ ...profiles[0], hasApiKey: false }, profiles[1]], scope))
})
test('rejects stale-invalid counters, threshold and unsafe numeric ids', () => {
  for (const complexInputCharacters of [0, 16001, 1.5, null]) assert.throws(() => modelRoutingPayload({ ...form(), complexInputCharacters }, profiles, scope))
  for (const revision of [-1, 2147483647, null]) assert.throws(() => modelRoutingPayload({ ...form(), revision }, profiles, scope))
  assert.throws(() => modelRoutingPayload({ ...form(), economyProfileId: 9007199254740992 }, profiles, scope))
  assert.throws(() => modelRoutingPayload({ ...form(), decisionProfileId: '9223372036854775808' }, profiles, scope))
})
test('rejects unselected space, absent flags and missing classifier profile', () => {
  assert.throws(() => modelRoutingPayload(form(), profiles, {}))
  assert.throws(() => modelRoutingPayload({ ...form(), enabled: null }, profiles, scope))
  assert.throws(() => modelRoutingPayload({ ...form(), decisionProfileId: '3' }, profiles, scope))
})
