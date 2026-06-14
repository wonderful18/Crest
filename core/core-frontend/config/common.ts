import path from 'path'
import fs from 'node:fs'
import { resolve } from 'path'
import Vue from '@vitejs/plugin-vue'
import eslintPlugin from 'vite-plugin-eslint'
import VueJsx from '@vitejs/plugin-vue-jsx'
import viteStylelint from 'vite-plugin-stylelint'
import {
  createStyleImportPlugin,
  ElementPlusSecondaryResolve
} from 'vite-plugin-style-import-secondary'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'
import svgLoader from 'vite-svg-loader'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components-secondary/vite'
import { ElementPlusResolver } from 'unplugin-vue-components-secondary/resolvers'
const root = process.cwd()
const elementPlusSecondaryStyleDeps = (() => {
  const componentsDir = path.resolve(root, 'node_modules/element-plus-secondary/es/components')
  if (!fs.existsSync(componentsDir)) {
    return []
  }
  return fs
    .readdirSync(componentsDir, { withFileTypes: true })
    .filter(item => {
      return item.isDirectory() && fs.existsSync(path.join(componentsDir, item.name, 'style/css.mjs'))
    })
    .map(item => `element-plus-secondary/es/components/${item.name}/style/css`)
})()
const skipViteLint = process.env.CREST_SKIP_VITE_LINT === 'true'
const lintPlugins = skipViteLint
  ? []
  : [
      eslintPlugin({
        cache: false,
        include: [
          'src/**/*.ts',
          'src/**/*.tsx',
          'src/**/*.js',
          'src/**/*.vue',
          'src/*.ts',
          'src/*.js',
          'src/*.vue'
        ]
      }),
      viteStylelint()
    ]

const patchElementPlusSecondaryTypes = (code: string) =>
  code
    .replace("import { inject } from 'vue';", "import { getCurrentInstance, inject } from 'vue';")
    .replace(
      'const customStyle = inject("$custom-style-filter", {});',
      'const customStyle = getCurrentInstance() ? inject("$custom-style-filter", {}) : {};'
    )

const elementPlusSecondaryTypesPatch = () => ({
  name: 'crest-element-plus-secondary-types-patch',
  enforce: 'pre',
  config() {
    return {
      optimizeDeps: {
        esbuildOptions: {
          plugins: [
            {
              name: 'crest-element-plus-secondary-types-patch',
              setup(build) {
                build.onLoad(
                  { filter: /element-plus-secondary[\\/]es[\\/]utils[\\/]types\.mjs$/ },
                  async args => {
                    const fs = await import('node:fs/promises')
                    const code = await fs.readFile(args.path, 'utf-8')
                    return {
                      contents: patchElementPlusSecondaryTypes(code),
                      loader: 'js'
                    }
                  }
                )
              }
            }
          ]
        }
      }
    }
  },
  transform(code: string, id: string) {
    if (!id.replace(/\\/g, '/').endsWith('/element-plus-secondary/es/utils/types.mjs')) {
      return null
    }
    return {
      code: patchElementPlusSecondaryTypes(code),
      map: null
    }
  }
})

export function pathResolve(dir: string) {
  // nosemgrep: javascript.lang.security.audit.path-traversal.path-join-resolve-traversal.path-join-resolve-traversal
  return resolve(root, '.', dir)
}
export default {
  base: './',
  plugins: [
    elementPlusSecondaryTypesPatch(),
    Vue(),
    svgLoader({
      svgo: false,
      defaultImport: 'component' // or 'raw'
    }),
    VueJsx(),
    createStyleImportPlugin({
      resolves: [ElementPlusSecondaryResolve()],
      libs: [
        {
          libraryName: 'element-plus-secondary',
          esModule: true,
          resolveStyle: name => {
            return `element-plus-secondary/es/components/${name.substring(3)}/style/css`
          }
        }
      ]
    }),
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    }),
    VueI18nPlugin({
      runtimeOnly: false,
      compositionOnly: true,
      include: [resolve(__dirname, 'src/locales/**')]
    }),
    ...lintPlugins
  ],
  css: {
    preprocessorOptions: {
      less: {
        modifyVars: {
          hack: `true; @import (reference) "${path.resolve('src/style/variable.less')}";`
        },
        javascriptEnabled: true
      }
    }
  },
  resolve: {
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.less', '.css'],
    alias: [
      {
        find: '@',
        replacement: `${pathResolve('src')}`
      }
    ]
  },
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'vue-types',
      'element-plus-secondary/es/locale/lang/zh-cn',
      'element-plus-secondary/es/locale/lang/en',
      '@vueuse/core',
      'axios',
      ...elementPlusSecondaryStyleDeps
    ]
  }
}
