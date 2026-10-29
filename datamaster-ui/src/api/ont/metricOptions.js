// 下拉选项沿用分页 API，遵守后端每页最多 100 条的限制。
export async function loadPagedOptions(loader, params = {}) {
  const rows = []
  let pageNum = 1
  while (true) {
    const response = await loader({ ...params, pageNum, pageSize: 100 })
    const page = response.data?.rows || []
    rows.push(...page)
    if (page.length < 100 || (response.data?.total != null && rows.length >= Number(response.data.total))) return rows
    pageNum += 1
  }
}
