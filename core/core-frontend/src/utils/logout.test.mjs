import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'logout.ts')

function loadLogoutUtils(origin = 'https://crest.example.com') {
  global.window = {
    location: {
      origin,
      href: `${origin}/casbi/index`,
      pathname: '/casbi/index',
      assign: () => {}
    }
  }

  const source = fs.readFileSync(sourcePath, 'utf8')
  const output = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020
    }
  }).outputText

  const mod = new Module(sourcePath)
  mod.filename = sourcePath
  mod.paths = Module._nodeModulePaths(path.dirname(sourcePath))

  const originalLoad = Module._load
  Module._load = request => {
    if (request === '@/store/modules/user') {
      return { useUserStoreWithOut: () => ({ clear: () => {}, $reset: () => {} }) }
    }
    if (request === '@/store/modules/permission') {
      return { usePermissionStoreWithOut: () => ({ clear: () => {}, $reset: () => {} }) }
    }
    if (request === '@/store/modules/interactive') {
      return { interactiveStoreWithOut: () => ({ clear: () => {}, $reset: () => {} }) }
    }
    if (request === '@/hooks/web/useCache') {
      return { useCache: () => ({ wsCache: { storage: {}, get: () => null, delete: () => {} } }) }
    }
    if (request === '@/router') {
      return { default: { currentRoute: { value: { fullPath: '/workbranch/index' } }, push: () => {} } }
    }
    return originalLoad(request)
  }

  try {
    mod._compile(output, sourcePath)
    return mod.exports
  } finally {
    Module._load = originalLoad
  }
}

describe('logout url helpers', () => {
  it('builds same-origin OIDC and CAS logout URLs', () => {
    const { buildSameOriginUrl } = loadLogoutUtils()

    assert.equal(
      buildSameOriginUrl('/crest', '/oidcbi/oidc/logout'),
      'https://crest.example.com/crest/oidcbi/oidc/logout'
    )
  })

  it('allows http logout URLs without embedded credentials', () => {
    const { safeLogoutUrl } = loadLogoutUtils()

    assert.equal(safeLogoutUrl('/logout'), 'https://crest.example.com/logout')
    assert.equal(safeLogoutUrl('https://idp.example.com/logout'), 'https://idp.example.com/logout')
  })

  it('rejects dangerous or credential-bearing logout URLs', () => {
    const { safeLogoutUrl } = loadLogoutUtils()

    assert.equal(safeLogoutUrl('javascript:alert(1)'), null)
    assert.equal(safeLogoutUrl('https://user:pass@example.com/logout'), null)
    assert.equal(safeLogoutUrl('ftp://example.com/logout'), null)
  })
})
