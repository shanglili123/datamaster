import request from '@/utils/request'

// ========== Function CRUD ==========
export function listFunction(query) {
  return request({ url: '/ont/function/page', method: 'get', params: query })
}
export function getFunctionsByOntology(ontologyId) {
  return request({ url: '/ont/function/list', method: 'get', params: { ontologyId } })
}
export function getFunction(id) {
  return request({ url: '/ont/function/' + id, method: 'get' })
}
export function addFunction(data) {
  return request({ url: '/ont/function', method: 'post', data })
}
export function updateFunction(data) {
  return request({ url: '/ont/function', method: 'put', data })
}
export function delFunction(id) {
  return request({ url: '/ont/function/' + id, method: 'delete' })
}

// ========== Function Execution ==========
export function submitFuncExecution(data) {
  return request({ url: '/ont/function/exec/submit', method: 'post', data })
}
export function approveFuncExecution(data) {
  return request({ url: '/ont/function/exec/approve', method: 'post', data })
}
export function rejectFuncExecution(data) {
  return request({ url: '/ont/function/exec/reject', method: 'post', data })
}
export function runFuncExecution(id) {
  return request({ url: '/ont/function/exec/run/' + id, method: 'post' })
}
export function listFuncExecution(query) {
  return request({ url: '/ont/function/exec/page', method: 'get', params: query })
}

// ========== Graph ==========
export function getGraphData(ontologyId) {
  return request({ url: '/ont/graph/data', method: 'get', params: { ontologyId } })
}
