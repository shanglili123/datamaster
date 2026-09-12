import request from '@/utils/request'

// AI 问数对话
export function askDataDbgptChat(data) {
  return request({
    url: '/ai/ask-data/dbgpt/chat',
    method: 'post',
    data: data
  })
}

// AI 问数报告生成
export function askDataDbgptReport(data, config = {}) {
  return request({
    url: '/ai/ask-data/dbgpt/report',
    method: 'post',
    data: data,
    ...config
  })
}
