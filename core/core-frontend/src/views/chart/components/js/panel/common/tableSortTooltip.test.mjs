import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getTooltipPointerPosition,
  getHeaderSortField,
  getHeaderSortStateId,
  hasTooltipContent,
  isSortTooltipRequest,
  sortTableRowsForHeader
} from './tableSortTooltip.mjs'

test('isSortTooltipRequest accepts legacy iconName sort clicks', () => {
  assert.equal(isSortTooltipRequest({ iconName: 'customSortDefault0.1' }), true)
})

test('isSortTooltipRequest accepts current S2 name sort clicks', () => {
  assert.equal(isSortTooltipRequest({ name: 'customSortDefault0.1' }), true)
})

test('isSortTooltipRequest accepts explicit sort menu requests', () => {
  assert.equal(isSortTooltipRequest({ sortMenu: true, name: 'anything' }), true)
})

test('isSortTooltipRequest ignores unrelated header icons', () => {
  assert.equal(isSortTooltipRequest({ name: 'customDownload' }), false)
})

test('getTooltipPointerPosition accepts complete pointer coordinates', () => {
  assert.deepEqual(getTooltipPointerPosition({ clientX: 12, clientY: 34 }), { x: 12, y: 34 })
})

test('getTooltipPointerPosition rejects incomplete pointer coordinates', () => {
  assert.equal(getTooltipPointerPosition({ clientX: 12 }), null)
  assert.equal(getTooltipPointerPosition({ clientY: 34 }), null)
  assert.equal(getTooltipPointerPosition({}), null)
})

test('hasTooltipContent accepts existing non-empty content', () => {
  assert.equal(hasTooltipContent({ content: '0' }), true)
  assert.equal(hasTooltipContent({ content: 0 }), true)
})

test('hasTooltipContent rejects empty content', () => {
  assert.equal(hasTooltipContent({ content: '' }), false)
  assert.equal(hasTooltipContent({ content: null }), false)
  assert.equal(hasTooltipContent({}), false)
})

test('getHeaderSortStateId uses S2 node id for icon state', () => {
  assert.equal(getHeaderSortStateId({ id: 'field-id', field: 'engine_field' }), 'field-id')
})

test('getHeaderSortField uses S2 field for row sort', () => {
  assert.equal(getHeaderSortField({ id: 'field-id', field: 'engine_field' }), 'engine_field')
})

test('sortTableRowsForHeader sorts numeric strings by numeric value', () => {
  const data = [{ value: '10' }, { value: '2' }, { value: '30' }]

  assert.deepEqual(
    sortTableRowsForHeader({ data, sortFieldId: 'value', sortMethod: 'asc' }).map(
      row => row.value
    ),
    ['2', '10', '30']
  )
})

test('sortTableRowsForHeader keeps summary row last', () => {
  const data = [{ value: '10' }, { value: '2' }, { SUMMARY: true, value: '12' }]

  assert.deepEqual(
    sortTableRowsForHeader({ data, sortFieldId: 'value', sortMethod: 'desc' }).map(
      row => row.value
    ),
    ['10', '2', '12']
  )
})
