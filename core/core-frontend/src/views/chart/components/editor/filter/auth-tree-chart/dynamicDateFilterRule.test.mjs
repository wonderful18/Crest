import test from 'node:test'
import assert from 'node:assert/strict'

import {
  DEFAULT_DYNAMIC_DATE_TERM,
  normalizeDynamicDateFilterRule
} from './dynamicDateFilterRule.mjs'

test('dynamic date preset defaults to equality when operator is missing', () => {
  const rule = normalizeDynamicDateFilterRule({
    fieldType: 1,
    filterTypeTime: 'dynamicDate',
    term: '',
    timeValue: '今年',
    dynamicTimeSetting: {
      timeGranularity: 'year',
      relativeToCurrent: 'thisYear'
    }
  })

  assert.equal(rule.term, DEFAULT_DYNAMIC_DATE_TERM)
})

test('dynamic date keeps an explicitly selected operator', () => {
  const rule = normalizeDynamicDateFilterRule({
    fieldType: 1,
    filterTypeTime: 'dynamicDate',
    term: 'ge',
    timeValue: '今年',
    dynamicTimeSetting: {
      timeGranularity: 'year',
      relativeToCurrent: 'thisYear'
    }
  })

  assert.equal(rule.term, 'ge')
})

test('fixed date filter still requires an explicit operator', () => {
  const rule = normalizeDynamicDateFilterRule({
    fieldType: 1,
    filterTypeTime: 'dateValue',
    term: '',
    timeValue: '2026/01'
  })

  assert.equal(rule.term, '')
})

test('dynamic date without a selected value is not silently completed', () => {
  const rule = normalizeDynamicDateFilterRule({
    fieldType: 1,
    filterTypeTime: 'dynamicDate',
    term: '',
    timeValue: ''
  })

  assert.equal(rule.term, '')
})

test('dynamic date label without dynamic setting is not silently completed', () => {
  const rule = normalizeDynamicDateFilterRule({
    fieldType: 1,
    filterTypeTime: 'dynamicDate',
    term: '',
    timeValue: '今年'
  })

  assert.equal(rule.term, '')
})
