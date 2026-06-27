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

// 执行SQL查询
export function executeSql(datasourceId, sql, maxRows = 1000) {
  return request({
    url: '/ai/ask-data/execute',
    method: 'post',
    params: {
      datasourceId: datasourceId,
      sql: sql,
      maxRows: maxRows
    }
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
