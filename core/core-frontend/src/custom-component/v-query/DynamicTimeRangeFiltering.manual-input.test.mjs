import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

const source = readFileSync(
  new URL('./DynamicTimeRangeFiltering.vue', import.meta.url),
  'utf8'
)

test('fixed time range configuration uses two editable date inputs', () => {
  assert.match(source, /rangeStartValue\s*=\s*computed/)
  assert.match(source, /rangeEndValue\s*=\s*computed/)
  assert.match(source, /v-model="rangeStartValue"/)
  assert.match(source, /v-model="rangeEndValue"/)
})
