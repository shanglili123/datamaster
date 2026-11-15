import request from '@/utils/request'
export const modelStats = params => request({ url: '/ai/model-gateway/stats', method: 'get', params })
export const listModelProfiles = params => request({ url: '/ai/model-profiles', method: 'get', params })
export const addModelProfile = data => request({ url: '/ai/model-profiles', method: 'post', data })
export const updateModelProfile = (id, data) => request({ url: `/ai/model-profiles/${id}`, method: 'put', data })
export const activateModelProfile = (id, params) => request({ url: `/ai/model-profiles/${id}/activate`, method: 'post', params })
export const deleteModelProfile = (id, params) => request({ url: `/ai/model-profiles/${id}`, method: 'delete', params })
export const getModelRouting = params => request({ url: '/ai/model-routing', method: 'get', params })
export const saveModelRouting = data => request({ url: '/ai/model-routing', method: 'put', data })
