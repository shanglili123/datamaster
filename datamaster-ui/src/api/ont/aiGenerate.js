import request from '@/utils/request'

// AI 生成本体 - 预览（不落库）
export function aiGeneratePreview(data) {
  return request({
    url: '/ont/ai-generate/preview',
    method: 'post',
    data: data
  })
}

// AI 生成本体 - 生成落库
export function aiGenerate(data) {
  return request({
    url: '/ont/ai-generate/generate',
    method: 'post',
    data: data
  })
}

// AI 生成本体 - 获取可选业务表（元数据目录，不直连数据源）
export function listAiGenerateTables(params) {
  return request({
    url: '/ont/ai-generate/tables',
    method: 'get',
    params: params
  })
}