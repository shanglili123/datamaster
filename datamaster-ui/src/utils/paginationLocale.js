/**
 * ant-design-vue a-pagination 中文文案
 *
 * 全工程分页统一中文化：
 * - 每页条数下拉显示为 "6/页"（而非默认 "6 条/页"），与产品要求一致
 * - 跳页提示改为 "跳转"（而非默认 "跳至"）
 * - 其余页码导航文案（上一页/下一页/向前 5 页 等）也一并本地化
 *
 * 字段名必须与 ant-design-vue vc-pagination/locale/en_US.js 保持一致：
 * items_per_page / jump_to / jump_to_confirm / page / prev_page / next_page / prev_5 / next_5 / prev_3 / next_3
 */
export default {
  // Options.jsx
  items_per_page: '/页',
  jump_to: '跳转',
  jump_to_confirm: '确定',
  page: '页',
  // Pagination.jsx
  prev_page: '上一页',
  next_page: '下一页',
  prev_5: '向前 5 页',
  next_5: '向后 5 页',
  prev_3: '向前 3 页',
  next_3: '向后 3 页'
}
