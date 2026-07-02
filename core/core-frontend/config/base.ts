import pkg from '../package.json'
import { gzipSync } from 'node:zlib'

const enableGzip = process.env.CREST_ENABLE_VITE_GZIP === 'true'
const GZIP_THRESHOLD = 10240
const emptyProxyPackages = new Set([
  'lodash-unified',
  'vue-demi',
  '@vue/devtools-api',
  '@vant/popperjs',
  '@floating-ui',
  '@floating-ui/dom',
  '@floating-ui/core',
  '@antv/vendor',
  'javascript-natural-sort',
  'escape-latex',
  'seedrandom',
  'tiny-emitter'
])

function vendorChunkName(id: string) {
  if (!id.includes('node_modules')) {
    return
  }
  const modulePath = id.toString().replace(/\\/g, '/').split('node_modules/').pop()
  if (!modulePath) {
    return
  }
  const parts = modulePath.split('/')
  const packageName = parts[0].startsWith('@') ? `${parts[0]}/${parts[1]}` : parts[0]
  if (emptyProxyPackages.has(packageName) || emptyProxyPackages.has(parts[0])) {
    return
  }
  return packageName
    .replace(/^@/, '')
    .replace(/[\\/]/g, '-')
    .replace(/[^a-zA-Z0-9._-]/g, '-')
    .replace(/^\.+/, 'vendor-')
}

function gzipAssetsPlugin() {
  return {
    name: 'crest-gzip-assets',
    apply: 'build',
    generateBundle(_, bundle) {
      Object.entries(bundle).forEach(([fileName, item]) => {
        if (fileName.endsWith('.gz') || item.type !== 'asset' && item.type !== 'chunk') {
          return
        }
        const source = item.type === 'chunk' ? item.code : item.source
        const input = typeof source === 'string' ? Buffer.from(source) : Buffer.from(source)
        if (input.length < GZIP_THRESHOLD) {
          return
        }
        this.emitFile({
          type: 'asset',
          fileName: `${fileName}.gz`,
          source: gzipSync(input)
        })
      })
    }
  }
}

export default {
  plugins: enableGzip ? [gzipAssetsPlugin()] : [],
  build: {
    reportCompressedSize: false,
    chunkSizeWarningLimit: 10000,
    rollupOptions: {
      output: {
        // 用于命名代码拆分时创建的共享块的输出命名
        chunkFileNames: `assets/chunk/[name]-[hash]-${pkg.version}-${pkg.name}.js`,
        assetFileNames: `assets/[ext]/[name]-[hash]-${pkg.version}-${pkg.name}.[ext]`,
        entryFileNames: `js/[name]-[hash]-${pkg.version}-${pkg.name}.js`,
        manualChunks(id: string) {
          return vendorChunkName(id)
        }
      }
    },
    sourcemap: false
  }
}
