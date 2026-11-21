import test from 'node:test'
import assert from 'node:assert/strict'
import MarkdownIt from 'markdown-it'
import { normalizeTableMarkdown, headerlessTablePlugin } from '../src/utils/tableMarkdown.js'

const md = new MarkdownIt().use(headerlessTablePlugin)
const render = text => md.render(normalizeTableMarkdown(text))

test('standard tables retain headers, rows and numeric formatting', () => {
  const html = render('| 名称 | 数量 |\n| --- | --- |\n| sales_order | 6 |\n| product | 4 |')
  assert.match(html, /<thead>/)
  assert.match(html, /<th>名称<\/th>/)
  assert.match(html, /<td>sales_order<\/td>/)
  assert.match(html, /<td>6<\/td>/)
})

test('unicode separator dashes become valid markdown without changing values', () => {
  const html = render('| 名称 | 单价 |\n| —— | ——— |\n| PROD—0001 | 10.00 |')
  assert.match(html, /<thead>/)
  assert.match(html, /PROD—0001/)
  assert.match(html, /10\.00/)
})

test('headerless tool tables render bodies without inventing column labels', () => {
  const html = render('| ——— | ——— |\n| CUST-0001 | 华光电子集团 |\n| CUST-0002 | 星辉重工 |')
  assert.match(html, /<tbody>/)
  assert.doesNotMatch(html, /<thead>|<th>/)
  assert.match(html, /<td>华光电子集团<\/td>/)
  assert.equal((html.match(/<tr>/g) || []).length, 2)
})

test('adjacent headerless result sets remain separate tables', () => {
  const html = render('| --- | --- |\n| sales_order | 6 |\n| --- | --- | --- |\n| CUST-0001 | 客户 | A |\n\n数据库不为空。')
  assert.equal((html.match(/<table>/g) || []).length, 2)
  assert.match(html, /<p>数据库不为空。<\/p>/)
})

test('a flattened single-column preview followed by the same result renders once', () => {
  const html = render('| --- | | 12 || company_count | | --- | | 12 |')
  assert.match(html, /<th>company_count<\/th>/)
  assert.equal((html.match(/<td>12<\/td>/g) || []).length, 1)
  const conflicting = '| --- | | 12 || company_count | | --- | | 13 |'
  assert.equal(normalizeTableMarkdown(conflicting), conflicting)
})

test('ambiguous flattened pipes and mismatched rows are not guessed', () => {
  const html = render('| --- | --- | | customer | 5 |')
  assert.doesNotMatch(html, /<table>/)
  assert.doesNotMatch(render('| --- | --- |\n| only one |'), /<table>/)
})

test('code fences and inline pipes are left alone', () => {
  const code = '```text\n| ——— | ——— |\n| a | b |\n```'
  assert.equal(normalizeTableMarkdown(code), code)
  assert.doesNotMatch(render(code), /<table>/)
  assert.equal(normalizeTableMarkdown('使用 a | b 进行说明'), '使用 a | b 进行说明')
})
