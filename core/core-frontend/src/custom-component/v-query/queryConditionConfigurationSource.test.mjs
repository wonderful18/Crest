import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const source = readFileSync(
  new URL('./QueryConditionConfiguration.vue', import.meta.url),
  'utf8'
)

test('opening an existing query condition keeps saved dataset fields visible while reloading', () => {
  assert.equal(
    source.includes('curComponent.value.dataset.fields = []'),
    false,
    'opening a saved condition must not clear dataset fields before async reload completes'
  )
})

test('initialization only syncs selected component state without recomputing field configuration', () => {
  assert.match(
    source,
    /finally\(\(\) => \{[\s\S]*syncCheckedFieldsStatus\(curComponent\.value\.checkedFields\)/,
    'async initialization should update checkbox state without calling the mutation-heavy change handler'
  )
  assert.doesNotMatch(
    source,
    /finally\(\(\) => \{[\s\S]*handleCheckedFieldsChange\(curComponent\.value\.checkedFields\)/,
    'opening saved configuration must not trigger display type and option field recalculation'
  )
})
