/*
 * Space report API.
 */

import { http } from '@/api/http'
import { httpErrorHandle } from '@/utils'
import { ContentTypeEnum, RequestHttpEnum, ModuleTypeEnum } from '@/enums/httpEnum'
import { SpaceItem, SpaceDetail } from './space'

// * 空间列表
export const spaceListApi = async (data: object) => {
  try {
    const res = await http(RequestHttpEnum.GET)<{
      list: SpaceItem[],
      count: number
    }>(`${ModuleTypeEnum.PROJECT}/my-page`, data)
    return res
  } catch {
    httpErrorHandle()
  }
}

// * 新增空间
export const createSpaceApi = async (data: object) => {
  try {
    const res = await http(RequestHttpEnum.POST)<number>(`${ModuleTypeEnum.PROJECT}/create`, data)
    return res
  } catch {
    httpErrorHandle()
  }
}

// * 获取空间
export const fetchSpaceApi = async (data: object) => {
  try {
    const res = await http(RequestHttpEnum.GET)<SpaceDetail>(`${ModuleTypeEnum.PROJECT}/get`, data)
    return res
  } catch {
    httpErrorHandle()
  }
}

// * 保存空间
export const saveSpaceApi = async (data: object) => {
  try {
    const res = await http(RequestHttpEnum.PUT)(`${ModuleTypeEnum.PROJECT}/update`, data)
    return res
  } catch {
    httpErrorHandle()
  }
}

// * 修改空间基础信息
export const updateSpaceApi = async (data: object) => {
  try {
    const res = await http(RequestHttpEnum.PUT)(`${ModuleTypeEnum.PROJECT}/update`, data)
    return res
  } catch {
    httpErrorHandle()
  }
}

// * 删除空间
export const deleteSpaceApi = async (data: object) => {
  try {
    const res = await http(RequestHttpEnum.DELETE)(`${ModuleTypeEnum.PROJECT}/delete`, data)
    return res
  } catch {
    httpErrorHandle()
  }
}

// * 修改发布状态 [0 已发布, 1 未发布]
export const changeSpaceReleaseApi = async (data: object) => {
  try {
    const res = await http(RequestHttpEnum.PUT)(`${ModuleTypeEnum.PROJECT}/update`, data)
    return res
  } catch {
    httpErrorHandle()
  }
}
