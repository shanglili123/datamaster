import request from '@/utils/request'

// 准备问数上下文
export function prepareAskData(data) {
  return request({
    url: '/ai/ask-data/prepare',
    method: 'post',
    data: data
  })
}

// 生成SQL
export function generateSql(data) {
  return request({
    url: '/ai/ask-data/sql',
    method: 'post',
    data: data
  })
}

// 一站式问数对话
export function askDataChat(data) {
  return request({
    url: '/ai/ask-data/chat',
    method: 'post',
    data: data
  })
}

// AI 问数对话
export function askDataDbgptChat(data) {
  return request({
    url: '/ai/ask-data/dbgpt/chat',
    method: 'post',
    data: data
  })
}

// AI 问数报告生成
export function askDataDbgptReport(data) {
  return request({
    url: '/ai/ask-data/dbgpt/report',
    method: 'post',
    data: data
  })
}
