// 下拉选项沿用分页 API；达到边界必须报错，不能静默返回不完整选项。
export async function loadPagedOptions(loader, params = {}, { isCurrent = () => true, maxPages = 100 } = {}) {
  if (!Number.isInteger(maxPages) || maxPages < 1 || maxPages > 100) throw new Error('选项分页上限无效')
  const rows = [], seen = new Set()
  for (let pageNum = 1; pageNum <= maxPages; pageNum += 1) {
    if (!isCurrent()) return []
    const response = await loader({ ...params, pageNum, pageSize: 100 })
    if (!isCurrent()) return []
    const page = response?.data?.rows
    if (!Array.isArray(page) || page.length > 100) throw new Error('选项分页响应无效，请刷新后重试')
    const rawTotal = response.data.total
    const total = rawTotal == null ? null : Number(rawTotal)
    if (total != null && (!['number', 'string'].includes(typeof rawTotal) || String(rawTotal).trim() === '' || !Number.isSafeInteger(total) || total < 0)) throw new Error('选项总数无效，请刷新后重试')
    for (const row of page) {
      if (!row || row.id == null) throw new Error('选项缺少有效标识，请刷新后重试')
      const key = String(row.id)
      if (seen.has(key)) throw new Error('选项分页发生重复或并发变化，请刷新后重试')
      seen.add(key); rows.push(row)
    }
    if (total != null && rows.length > total) throw new Error('选项分页总数已变化，请刷新后重试')
    if (total != null && rows.length === total) return rows
    if (page.length < 100) {
      if (total != null) throw new Error('选项分页不完整，请刷新后重试')
      return rows
    }
  }
  throw new Error('选项数量超出加载上限，请缩小范围后重试')
}
