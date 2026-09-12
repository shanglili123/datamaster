import request from '@/utils/request'

// 查询 Skill 列表
export function listSkill(query) {
  return request({
    url: '/ai/skill/page',
    method: 'get',
    params: query
  })
}

// 查询 Skill 详情
export function getSkill(id) {
  return request({
    url: '/ai/skill/' + id,
    method: 'get'
  })
}

// 新增 Skill
export function addSkill(data) {
  return request({
    url: '/ai/skill',
    method: 'post',
    data: data
  })
}

// 修改 Skill
export function updateSkill(data) {
  return request({
    url: '/ai/skill',
    method: 'put',
    data: data
  })
}

// 归档 Skill
export function delSkill(id) {
  return request({
    url: '/ai/skill/' + id,
    method: 'delete'
  })
}

// 发布 Skill
export function publishSkill(id) {
  return request({
    url: '/ai/skill/' + id + '/publish',
    method: 'post'
  })
}

// 回滚 Skill 版本
export function rollbackSkill(id, version) {
  return request({
    url: '/ai/skill/' + id + '/rollback/' + version,
    method: 'post'
  })
}

// 查询 Skill 版本
export function listSkillVersions(id) {
  return request({
    url: '/ai/skill/' + id + '/versions',
    method: 'get'
  })
}

// 生成元数据平台 Skill
export function generateMetadataSkill(params) {
  return request({
    url: '/ai/skill/generate/metadata',
    method: 'post',
    params: params
  })
}

// 生成质量平台 Skill
export function generateQualitySkill(params) {
  return request({
    url: '/ai/skill/generate/quality',
    method: 'post',
    params: params
  })
}

// 生成表级 Skill
export function generateTableSkill(data) {
  return request({
    url: '/ai/skill/generate/table',
    method: 'post',
    data: data
  })
}

// 生成整库 Skill
export function generateDatabaseSkill(data) {
  return request({
    url: '/ai/skill/generate/database',
    method: 'post',
    data: data
  })
}

// 生成多表 Skill
export function generateMultiTableSkill(data) {
  return request({
    url: '/ai/skill/generate/multi-table',
    method: 'post',
    data: data
  })
}

// 生成本体决策 Skill
export function generateOntologyDecisionSkill(data) {
  return request({
    url: '/ai/skill/generate/ontology-decision',
    method: 'post',
    data: data
  })
}

// 同步全部已发布 Skill 到决策智能体（底层适配器路由保持兼容）
export function syncAllSkillToDbgpt() {
  return request({
    url: '/ai/skill/sync/dbgpt',
    method: 'post'
  })
}

// 同步指定 Skill 到决策智能体（底层适配器路由保持兼容）
export function syncSkillToDbgpt(id) {
  return request({
    url: '/ai/skill/' + id + '/sync/dbgpt',
    method: 'post'
  })
}

// 查询 Skill 报告模板
export function listSkillReportTemplates(skillId) {
  return request({
    url: '/ai/skill/' + skillId + '/report-templates',
    method: 'get'
  })
}

// 根据自然语言需求生成 Skill 报告模板
export function generateSkillReportTemplate(skillId, data) {
  return request({
    url: '/ai/skill/' + skillId + '/report-templates/generate',
    method: 'post',
    data: data
  })
}

// 新增 Skill 报告模板
export function addSkillReportTemplate(skillId, data) {
  return request({
    url: '/ai/skill/' + skillId + '/report-templates',
    method: 'post',
    data: data
  })
}

// 修改 Skill 报告模板
export function updateSkillReportTemplate(skillId, templateId, data) {
  return request({
    url: '/ai/skill/' + skillId + '/report-templates/' + templateId,
    method: 'put',
    data: data
  })
}

// 删除 Skill 报告模板
export function deleteSkillReportTemplate(skillId, templateId) {
  return request({
    url: '/ai/skill/' + skillId + '/report-templates/' + templateId,
    method: 'delete'
  })
}

// 设置默认 Skill 报告模板
export function setDefaultSkillReportTemplate(skillId, templateId) {
  return request({
    url: '/ai/skill/' + skillId + '/report-templates/' + templateId + '/default',
    method: 'post'
  })
}
