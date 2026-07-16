export function normalizePage(response = {}) {
  const data = response.data || {};
  const rows = response.rows || data.rows || data.list || data.records || (Array.isArray(data) ? data : []);
  const total = Number(response.total ?? data.total ?? data.totalElements ?? rows.length);

  return {
    rows: Array.isArray(rows) ? rows : [],
    total: Number.isNaN(total) ? 0 : total,
  };
}

export function pageRows(rows = [], total = 0, query = {}) {
  const pageNum = Number(query.pageNum || query.current || 1);
  const pageSize = Number(query.pageSize || query.size || rows.length || 0);

  if (!pageSize || rows.length <= pageSize || total !== rows.length) {
    return rows;
  }

  const start = (pageNum - 1) * pageSize;
  return rows.slice(start, start + pageSize);
}
