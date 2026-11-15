/** Choose only from the authenticated user's current accessible inventory. */
export function selectAccessibleSpace(spaces, preferredId) {
  if (!Array.isArray(spaces)) throw new Error('空间列表响应无效')
  const available = spaces.filter(space => space && space.id != null && String(space.id) !== ''
    && space.validFlag !== false && space.validFlag !== '0'
    && space.delFlag !== true && space.delFlag !== '1'
    && String(space.code || space.spaceCode || '').trim())
  return available.find(space => String(space.id) === String(preferredId)) || available[0] || null
}
