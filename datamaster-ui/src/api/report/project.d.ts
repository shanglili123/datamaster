/*
 * Project report type declarations.
 */

export type ProjectItem = {
  /**
   * 空间 id
   */
  id: string
  /**
   * 空间名称
   */
  name: string
  /**
   * 空间状态:
   *
   * 0 - 已发布
   * 1 - 未发布
   */
  status: number
  /**
   * 创建时间
   */
  createTime: number
  /**
   * 预览图片 URL
   */
  picUrl: string
  /**
   * 创建者
   */
  creator: string
  /**
   * 空间备注
   */
  remark: string
}

export interface ProjectDetail extends ProjectItem {
  /**
   * 空间参数
   */
   content: string
}
