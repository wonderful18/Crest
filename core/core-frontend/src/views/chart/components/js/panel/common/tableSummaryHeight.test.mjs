import test from 'node:test'
import assert from 'node:assert/strict'

import {
  compactSummaryTableHeight,
  resolveS2ConfiguredHeight
} from './tableSummaryHeight.mjs'

test('resolveS2ConfiguredHeight uses S2 options height without requiring private container config', () => {
  assert.equal(resolveS2ConfiguredHeight({ options: { height: 240 } }), 240)
})

test('resolveS2ConfiguredHeight falls back to legacy container height', () => {
  assert.equal(resolveS2ConfiguredHeight({ container: { cfg: { height: 180 } } }), 180)
  assert.equal(
    resolveS2ConfiguredHeight({ options: { height: 'abc' }, container: { cfg: { height: 180 } } }),
    180
  )
})

test('resolveS2ConfiguredHeight ignores missing or invalid heights', () => {
  assert.equal(resolveS2ConfiguredHeight({}), 0)
  assert.equal(resolveS2ConfiguredHeight({ options: { height: 0 } }), 0)
  assert.equal(resolveS2ConfiguredHeight({ options: { height: 'abc' } }), 0)
})

test('compactSummaryTableHeight only shrinks when content is lower than configured height', () => {
  assert.equal(compactSummaryTableHeight(120, 200), 128)
  assert.equal(compactSummaryTableHeight(196, 200), 196)
  assert.equal(compactSummaryTableHeight(220, 200), null)
})
