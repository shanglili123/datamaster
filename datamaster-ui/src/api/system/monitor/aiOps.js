import request from '@/utils/request.js'

// AI运维诊断
export function diagnoseAiOps() {
  return request({
    url: '/monitor/ai-ops/diagnose',
    method: 'get'
  })
}
