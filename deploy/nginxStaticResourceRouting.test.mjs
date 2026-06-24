import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

const configs = ['deploy/split/nginx/crest.conf', 'deploy/kubernetes/base/configmap-nginx.yaml']

for (const configPath of configs) {
  test(`${configPath} proxies API static resources before asset regex locations`, () => {
    const config = readFileSync(configPath, 'utf8')

    assert.match(
      config,
      /location\s+\^~\s+\/api\/v1\/\s*\{/,
      'API routes must use a ^~ prefix location so uploaded images are proxied before static file regex locations'
    )
    assert.doesNotMatch(
      config,
      /location\s+\/api\/v1\/\s*\{/,
      'Plain API prefix locations can be overridden by .png/.jpg/.svg regex locations'
    )
    assert.match(
      config,
      /location\s+~\*\s+\\\.\(\?:js\|css\|png\|jpg\|jpeg\|gif\|svg\|ico\|webp\|woff2\?\|ttf\)\$/,
      'Static asset regex location should remain limited to frontend files after API proxy precedence is protected'
    )
  })
}
