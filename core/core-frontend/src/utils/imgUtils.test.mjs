import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import { describe, it } from 'node:test'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sourcePath = path.join(__dirname, 'imgUtils.ts')

function loadImgUtils() {
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
  Module._load = (request, parent, isMain) => {
    if (request === 'html2canvas') {
      return () => Promise.resolve({ toDataURL: () => '' })
    }
    if (request === 'jspdf') {
      return function JsPDF() {}
    }
    if (request === 'file-saver') {
      return { saveAs: () => {} }
    }
    if (request === 'modern-screenshot') {
      return { domToPng: () => Promise.resolve('') }
    }
    if (request === '@/store/modules/data-visualization/dvMain') {
      return { dvMainStoreWithOut: () => ({}) }
    }
    if (request === '@/store/modules/embedded') {
      return { useEmbedded: () => ({ baseUrl: '' }) }
    }
    if (request === 'pinia') {
      return {
        storeToRefs: () => ({
          canvasStyleData: { value: {} },
          componentData: { value: [] },
          canvasViewInfo: { value: {} },
          canvasViewDataInfo: { value: {} },
          dvInfo: { value: {} }
        })
      }
    }
    if (request === '@/api/staticResource') {
      return { resourceBase64: () => Promise.resolve({ data: [] }) }
    }
    if (request === '@/utils/utils') {
      return { deepCopy: value => JSON.parse(JSON.stringify(value)) }
    }
    if (request === '@/utils/canvasUtils') {
      return { initCanvasDataPrepare: () => {} }
    }
    return originalLoad(request, parent, isMain)
  }

  try {
    mod._compile(output.replace('import.meta.env.VITE_API_BASEPATH', "'/api'"), sourcePath)
    return mod.exports
  } finally {
    Module._load = originalLoad
  }
}

describe('imgUtils', () => {
  it('collects static resources from Group and nested Tabs children', () => {
    const { findStaticSourceInner } = loadImgUtils()
    const resources = []
    findStaticSourceInner(
      [
        {
          component: 'Group',
          commonBackground: { outerImage: 'static-resource/group-bg.png' },
          propValue: [
            {
              component: 'Picture',
              commonBackground: {},
              propValue: { url: 'static-resource/picture.png' }
            },
            {
              component: 'Tabs',
              commonBackground: {},
              propValue: [
                {
                  componentData: [
                    {
                      component: 'UserView',
                      innerType: 'picture-group',
                      commonBackground: {},
                      propValue: {
                        urlList: [{ url: 'static-resource/album.png' }]
                      }
                    }
                  ]
                }
              ]
            }
          ]
        }
      ],
      resources
    )

    assert.deepEqual(resources, [
      'static-resource/group-bg.png',
      'static-resource/picture.png',
      'static-resource/album.png'
    ])
  })

  it('skips incomplete nested component data without stopping template export', () => {
    const { findStaticSourceInner } = loadImgUtils()
    const resources = []

    findStaticSourceInner(
      [
        {
          component: 'Group',
          propValue: [
            {
              component: 'UserView',
              innerType: 'picture-group'
            },
            {
              component: 'Tabs'
            },
            {
              component: 'Picture',
              propValue: { url: 'static-resource/valid.png' }
            }
          ]
        }
      ],
      resources
    )

    assert.deepEqual(resources, ['static-resource/valid.png'])
  })
})
