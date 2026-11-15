export function modelRoutingPayload(form, profiles, scope) {
  const require = (condition, text) => { if (!condition) throw new Error(text) }
  require(scope?.spaceId != null && String(scope.spaceId).match(/^[1-9]\d*$/) && scope.spaceCode, '请先选择当前空间')
  require(typeof form.enabled === 'boolean' && typeof form.semanticDecisionEnabled === 'boolean' && typeof form.strictJsonSchema === 'boolean', '请明确路由开关')
  require(Number.isInteger(form.revision) && form.revision >= 0 && form.revision < 2147483647, '路由版本无效或已达上限，请重新加载')
  require(Number.isInteger(form.complexInputCharacters) && form.complexInputCharacters >= 1 && form.complexInputCharacters <= 16000, '复杂输入字符阈值必须在1到16000之间')
  const id = value => {
    if (value == null || value === '') return null
    require(typeof value === 'string' || Number.isSafeInteger(value), '模型ID精度无效')
    const result = String(value)
    require(/^[1-9]\d*$/.test(result) && BigInt(result) <= 9223372036854775807n, '模型ID无效')
    require(profiles.some(profile => String(profile.id) === result && profile.enabled === true && profile.hasApiKey === true), '所选模型未启用、缺少凭据或已失效，请刷新模型配置')
    return result
  }
  const economyProfileId = id(form.economyProfileId), advancedProfileId = id(form.advancedProfileId), decisionProfileId = id(form.decisionProfileId)
  require(!form.enabled || economyProfileId && advancedProfileId, '启用路由必须选择经济模型和高级模型')
  return { spaceId: scope.spaceId, spaceCode: scope.spaceCode, revision: form.revision, enabled: form.enabled,
    economyProfileId, advancedProfileId, decisionProfileId, semanticDecisionEnabled: form.semanticDecisionEnabled,
    strictJsonSchema: form.strictJsonSchema, complexInputCharacters: form.complexInputCharacters }
}
