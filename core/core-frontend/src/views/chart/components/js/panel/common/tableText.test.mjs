import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildTableCopyHtml,
  buildTableCopyText,
  createTableCopyTransformer,
  getTableCellCopyText,
  normalizeTableCellText
} from './tableText.mjs'

test('normalizes escaped newline and tab text for table display', () => {
  assert.equal(normalizeTableCellText('第一行\\n第二行\\t结尾'), '第一行\n第二行 结尾')
})

test('normalizes common html line breaks without rendering html', () => {
  assert.equal(normalizeTableCellText('A<br/>B&nbsp;<b>C</b>'), 'A\nB C')
})

test('removes dangerous html tags from table text', () => {
  assert.equal(normalizeTableCellText('A<script>alert(1)</script>B<iframe src=x>C'), 'ABC')
})

test('normalizes encoded html line breaks and whitespace entities', () => {
  assert.equal(normalizeTableCellText('A&lt;br&gt;B&#10;C&#9;D'), 'A\nB\nC D')
})

test('keeps single-cell copy readable and matrix copy tabular', () => {
  assert.equal(getTableCellCopyText('A\\nB', { preserveLineBreaks: true }), 'A\nB')
  assert.equal(
    buildTableCopyText([
      ['A\\nB', 'C\\tD'],
      ['<p>E</p>', 'F']
    ]),
    'A B\tC D\nE\tF'
  )
  assert.equal(
    buildTableCopyText(
      [
        ['A\\nB', 'C']
      ],
      [['标题 1', '标题\\t2']]
    ),
    '标题 1\t标题 2\nA B\tC'
  )
  assert.equal(
    buildTableCopyHtml(
      [
        ['A<br/>B', '<C>']
      ],
      [['标题']]
    ),
    '<meta charset="utf-8"><table><tbody><tr><th>标题</th></tr><tr><td>A B</td><td>&lt;C&gt;</td></tr></tbody></table>'
  )
})

test('normalizes S2 clipboard transformers for plain text and html', () => {
  const transformer = createTableCopyTransformer({
    'text/html': matrix => ({
      type: 'text/html',
      content: matrix.map(row => row.join('|')).join(';')
    })
  })

  assert.equal(transformer['text/plain']([['A<br/>B', 'C\\tD']]).content, 'A B\tC D')
  assert.equal(transformer['text/html']([['A<br/>B', 'C\\tD']]).content, 'A B|C D')
})
