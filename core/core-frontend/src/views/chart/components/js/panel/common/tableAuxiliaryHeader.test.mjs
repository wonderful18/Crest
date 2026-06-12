import test from 'node:test'
import assert from 'node:assert/strict'

import {
  AUXILIARY_HEADER_FLAG,
  buildHeaderCopyRows,
  buildS2HeaderColumns,
  getLeafColumnFields,
  hasAuxiliaryDescription,
  isAuxiliaryHeaderField,
  normalizeAuxiliaryDescriptions
} from './tableAuxiliaryHeader.mjs'

test('keeps plain table columns unchanged when header description is disabled', () => {
  assert.deepEqual(buildS2HeaderColumns(['month', 'count'], [], {}), ['month', 'count'])
})

test('adds a description level for plain table columns', () => {
  const columns = buildS2HeaderColumns(
    ['month', 'count'],
    [],
    { month: '投产周期', count: '需求拟稿会签耗时' },
    {
      enabled: true,
      descriptions: [{ field: 'count', text: '需求发起提出至获得审批的周期 T1' }]
    }
  )

  assert.equal(columns.length, 2)
  assert.equal(columns[0].title, '投产周期')
  assert.equal(columns[0].children[0].field, 'month')
  assert.equal(columns[0].children[0][AUXILIARY_HEADER_FLAG], true)
  assert.equal(columns[1].children[0].title, '需求发起提出至获得审批的周期 T1')
})

test('uses group names and replaces only leaf titles when grouped headers are present', () => {
  const columns = buildS2HeaderColumns(
    [
      {
        key: 'draft_group',
        children: [{ key: 'draft_submit' }, { key: 'draft_review' }]
      }
    ],
    [{ field: 'draft_group', name: '需求拟稿会签耗时' }],
    {
      draft_submit: '提出',
      draft_review: '受理'
    },
    {
      enabled: true,
      descriptions: [
        { field: 'draft_submit', text: '提出至审批周期 T1' },
        { field: 'draft_review', text: '受理至评审周期 T2' }
      ]
    }
  )

  assert.equal(columns[0].field, 'draft_group')
  assert.equal(columns[0].title, '需求拟稿会签耗时')
  assert.equal(columns[0].children[0].title, '提出至审批周期 T1')
  assert.equal(columns[0].children[1][AUXILIARY_HEADER_FLAG], true)
  assert.deepEqual(getLeafColumnFields(columns), ['draft_submit', 'draft_review'])
})

test('normalizes descriptions by current visible fields', () => {
  const auxiliaryHeader = {
    enabled: true,
    descriptions: [
      { field: 'a', text: 'A' },
      { field: 'hidden', text: 'hidden' }
    ]
  }

  assert.deepEqual(normalizeAuxiliaryDescriptions(auxiliaryHeader, ['a', 'b']), [
    { field: 'a', text: 'A' },
    { field: 'b', text: '' }
  ])
  assert.equal(hasAuxiliaryDescription(auxiliaryHeader, ['a', 'b']), true)
  assert.equal(hasAuxiliaryDescription(auxiliaryHeader, ['b']), false)
})

test('builds copy header rows with grouped descriptions', () => {
  const columns = buildS2HeaderColumns(
    [
      {
        key: 'draft_group',
        children: [{ key: 'draft_submit' }, { key: 'draft_review' }]
      }
    ],
    [{ field: 'draft_group', name: '需求拟稿会签耗时' }],
    {
      draft_submit: '提出',
      draft_review: '受理'
    },
    {
      enabled: true,
      descriptions: [
        { field: 'draft_submit', text: '提出至审批周期 T1' },
        { field: 'draft_review', text: '受理至评审周期 T2' }
      ]
    }
  )

  assert.deepEqual(buildHeaderCopyRows(columns, ['draft_submit', 'draft_review']), [
    ['需求拟稿会签耗时', '需求拟稿会签耗时'],
    ['提出至审批周期 T1', '受理至评审周期 T2']
  ])
})

test('builds copy header rows for plain fields with descriptions', () => {
  const columns = buildS2HeaderColumns(
    ['month', 'count'],
    [],
    { month: '投产周期', count: '需求拟稿会签耗时' },
    {
      enabled: true,
      descriptions: [{ field: 'count', text: '需求发起提出至获得审批的周期 T1' }]
    }
  )

  assert.deepEqual(buildHeaderCopyRows(columns, ['month', 'count']), [
    ['投产周期', '需求拟稿会签耗时'],
    ['', '需求发起提出至获得审批的周期 T1']
  ])
})

test('detects auxiliary description leaf fields', () => {
  const columns = buildS2HeaderColumns(
    [
      {
        key: 'draft_group',
        children: [{ key: 'draft_submit' }, { key: 'draft_review' }]
      }
    ],
    [{ field: 'draft_group', name: '需求拟稿会签耗时' }],
    {},
    {
      enabled: true,
      descriptions: [{ field: 'draft_submit', text: '提出至审批周期 T1' }]
    }
  )

  assert.equal(isAuxiliaryHeaderField(columns, 'draft_submit'), true)
  assert.equal(isAuxiliaryHeaderField(columns, 'draft_group'), false)
  assert.equal(isAuxiliaryHeaderField(['month'], 'month'), false)
})
