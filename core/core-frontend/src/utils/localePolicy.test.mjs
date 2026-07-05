import test from 'node:test'
import assert from 'node:assert/strict'
import {
  DEFAULT_LOCALE,
  normalizeBuiltInLocale,
  resolveInitialLocale
} from './localePolicy.mjs'

test('defaults to simplified Chinese when no user language is stored', () => {
  assert.equal(resolveInitialLocale(), DEFAULT_LOCALE)
  assert.equal(resolveInitialLocale(''), DEFAULT_LOCALE)
  assert.equal(resolveInitialLocale('   '), DEFAULT_LOCALE)
})

test('normalizes built-in locale aliases', () => {
  assert.equal(resolveInitialLocale('zh_CN'), 'zh-CN')
  assert.equal(resolveInitialLocale('zh-Hans-CN'), 'zh-CN')
  assert.equal(resolveInitialLocale('zh-TW'), 'tw')
  assert.equal(resolveInitialLocale('en-US'), 'en')
  assert.equal(resolveInitialLocale('en_GB'), 'en')
})

test('preserves custom locale codes configured by the server', () => {
  assert.equal(resolveInitialLocale('custom-business'), 'custom-business')
  assert.equal(normalizeBuiltInLocale('custom-business'), null)
})
