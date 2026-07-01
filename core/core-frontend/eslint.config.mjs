import globals from 'globals'
import tsPlugin from '@typescript-eslint/eslint-plugin'
import tsParser from '@typescript-eslint/parser'
import vuePlugin from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'
import prettierConfig from 'eslint-config-prettier'

export default [
  {
    ignores: [
      'dist/**',
      '*.js',
      'node_modules/**',
      'dist*/**',
      '**/*.mjs',
      'src/main.ts',
      'flushbonading/**',
      'src/assets/fsSvg.js',
      'vendor/**'
    ]
  },
  ...vuePlugin.configs['flat/essential'],
  ...tsPlugin.configs['flat/recommended'],
  {
    files: ['**/*.{vue,ts,tsx,js,jsx}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.es2021
      },
      parser: vueParser,
      parserOptions: {
        parser: tsParser,
        warnOnUnsupportedTypeScriptVersion: false,
        sourceType: 'module',
        jsxPragma: 'React',
        ecmaFeatures: {
          jsx: true
        }
      }
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      '@typescript-eslint/no-empty-object-type': 'off',
      '@typescript-eslint/no-unused-expressions': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
      '@typescript-eslint/no-non-null-assertion': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      'vue/no-setup-props-destructure': 'off'
    }
  },
  prettierConfig
]
