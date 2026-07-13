import request from '@/utils/request'

export function listAskSessions(params) {
  return request({
    url: '/ai/ask-session',
    method: 'get',
    params
  })
}

export function createAskSession(data) {
  return request({
    url: '/ai/ask-session',
    method: 'post',
    data
  })
}

export function updateAskSession(sessionId, data) {
  return request({
    url: `/ai/ask-session/${sessionId}`,
    method: 'put',
    data
  })
}

export function deleteAskSession(sessionId, params) {
  return request({
    url: `/ai/ask-session/${sessionId}`,
    method: 'delete',
    params
  })
}

export function listAskMessages(sessionId, params) {
  return request({
    url: `/ai/ask-session/${sessionId}/messages`,
    method: 'get',
    params
  })
}

export function appendAskMessage(sessionId, data, params) {
  return request({
    url: `/ai/ask-session/${sessionId}/messages`,
    method: 'post',
    params,
    data
  })
}

export function deleteAskMessage(sessionId, messageId, params) {
  return request({
    url: `/ai/ask-session/${sessionId}/messages/${messageId}`,
    method: 'delete',
    params
  })
}

export function clearAskMessages(sessionId, params) {
  return request({
    url: `/ai/ask-session/${sessionId}/messages`,
    method: 'delete',
    params
  })
}
