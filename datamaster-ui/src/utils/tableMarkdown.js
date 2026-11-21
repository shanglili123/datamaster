// Repair presentation syntax only; never invent column labels, values or ambiguous row boundaries.
export function normalizeTableMarkdown(content = '') {
  let fence = null
  return String(content).replace(/\r\n?/g, '\n').split('\n').map(line => {
    const marker = line.match(/^\s*(`{3,}|~{3,})/)
    if (marker) {
      if (!fence) fence = marker[1]
      else if (marker[1][0] === fence[0] && marker[1].length >= fence.length) fence = null
      return line
    }
    if (fence || !line.includes('|')) return line
    // Some adapters use em/en/full-width dashes for table separators.
    if (/^\s*\|?\s*:?[\-—–−－]{1,}:?\s*(?:\|\s*:?[\-—–−－]{1,}:?\s*)+\|?\s*$/.test(line)) {
      return line.replace(/([\-—–−－]+)/g, '---')
    }
    return repairSingleColumnSnapshot(line)
  }).join('\n')
}

// Recover a single-column snapshot when a headerless preview is immediately
// followed by the same value in a complete table. Leave ambiguous rows alone.
function repairSingleColumnSnapshot(line) {
  const match = line.match(/^\s*\|\s*[-—–−－]{3,}\s*\|\s*\|\s*([^|`\\]+?)\s*\|\s*\|\s*([^|`\\]+?)\s*\|\s*\|\s*[-—–−－]{3,}\s*\|\s*\|\s*([^|`\\]+?)\s*\|\s*$/)
  if (!match || match[1].trim() !== match[3].trim()) return line
  return `| ${match[2].trim()} |\n| --- |\n| ${match[3].trim()} |`
}

function tableCells(line) {
  const text = line.trim()
  // Only explicit outer pipes are safe for headerless tool output.
  if (!text.startsWith('|') || !text.endsWith('|') || text.includes('\\|') || text.includes('`')) return null
  return text.slice(1, -1).split('|').map(cell => cell.trim())
}

// Tool output sometimes contains only a separator and data rows. Keep them as tbody
// rather than guessing a header or showing the pipe syntax as a paragraph.
export function headerlessTablePlugin(md) {
  md.block.ruler.before('table', 'tool_table', (state, startLine, endLine, silent) => {
    if (state.sCount[startLine] - state.blkIndent >= 4) return false
    const lineAt = index => state.src.slice(state.bMarks[index] + state.tShift[index], state.eMarks[index])
    const columns = tableCells(lineAt(startLine))
    if (!columns || columns.length < 2 || !columns.every(cell => /^:?-{3,}:?$/.test(cell))) return false
    const rows = []
    let next = startLine + 1
    for (; next < endLine; next += 1) {
      if (state.sCount[next] < state.blkIndent || state.sCount[next] - state.blkIndent >= 4) break
      const cells = tableCells(lineAt(next))
      if (!cells || cells.length !== columns.length || cells.every(cell => /^:?-{3,}:?$/.test(cell))) break
      rows.push(cells)
    }
    if (!rows.length) return false
    if (silent) return true
    const open = state.push('table_open', 'table', 1)
    open.map = [startLine, next]
    state.push('tbody_open', 'tbody', 1)
    rows.forEach(cells => {
      state.push('tr_open', 'tr', 1)
      cells.forEach(cell => {
        state.push('td_open', 'td', 1)
        const token = state.push('inline', '', 0)
        token.content = cell
        token.children = []
        state.push('td_close', 'td', -1)
      })
      state.push('tr_close', 'tr', -1)
    })
    state.push('tbody_close', 'tbody', -1)
    state.push('table_close', 'table', -1)
    state.line = next
    return true
  }, { alt: ['paragraph'] })
}
