import request from '@/utils/request'

export function getIngestionConfig() {
  return request({ url: '/ingestion/config', method: 'get' })
}

export function getIngestionRuntime() {
  return request({ url: '/ingestion/runtime', method: 'get' })
}

export function pauseIngestionConsumer() {
  return request({ url: '/ingestion/pause', method: 'post' })
}

export function resumeIngestionConsumer() {
  return request({ url: '/ingestion/resume', method: 'post' })
}

export function getIngestionMetrics() {
  return request({ url: '/ingestion/metrics', method: 'get' })
}

export function listIngestionDeadLetters(params) {
  return request({ url: '/ingestion/dead-letters', method: 'get', params })
}

export function replayIngestionDeadLetter(logId) {
  return request({ url: '/ingestion/dead-letters/replay', method: 'post', data: { logId } })
}

export function replayIngestionDeadLetters(logIds) {
  return request({ url: '/ingestion/dead-letters/replay-batch', method: 'post', data: { logIds } })
}
