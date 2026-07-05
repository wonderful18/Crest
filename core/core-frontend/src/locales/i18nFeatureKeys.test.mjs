import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import Module from 'node:module'
import path from 'node:path'
import ts from 'typescript'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const requiredKeys = [
  'chart.summary_column',
  'chart.summary_column_name',
  'chart.summary_column_field',
  'chart.summary_column_unit',
  'chart.summary_column_empty_text',
  'chart.summary_column_no_quota',
  'chart.summary_column_field_required',
  'chart.summary_column_formula_required',
  'chart.value_formatter_percent',
  'chart.value_formatter_thousand_separator',
  'visualization.background_card',
  'visualization.add_background_card',
  'visualization.background_card_select_more',
  'visualization.background_card_create_failed',
  'visualization.chart_group_container',
  'visualization.chart_group_name',
  'visualization.chart_group_not_joined',
  'visualization.chart_group_joined',
  'visualization.chart_group_create',
  'visualization.chart_group_create_selected',
  'visualization.chart_group_edit',
  'visualization.chart_group_remove',
  'visualization.chart_group_selected_count',
  'visualization.chart_group_layout_mode',
  'visualization.chart_group_layout_keep',
  'visualization.chart_group_layout_horizontal',
  'visualization.chart_group_layout_vertical',
  'visualization.chart_group_layout_grid',
  'visualization.chart_group_horizontal_gap',
  'visualization.chart_group_vertical_gap',
  'visualization.chart_group_clip_overflow',
  'visualization.chart_group_apply_layout',
  'visualization.chart_group_cancel',
  'visualization.chart_group_grid_unavailable',
  'visualization.chart_group_create_failed',
  'visualization.chart_group_select_more',
  'visualization.chart_group_parent_missing'
]

const getValue = (locale, key) =>
  key.split('.').reduce((value, name) => (value == null ? value : value[name]), locale)

const loadLocale = lang => {
  const sourcePath = path.join(__dirname, `${lang}.ts`)
  const output = ts.transpileModule(fs.readFileSync(sourcePath, 'utf8'), {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020
    }
  }).outputText
  const mod = new Module(sourcePath)
  mod.filename = sourcePath
  mod.paths = Module._nodeModulePaths(path.dirname(sourcePath))
  mod._compile(output, sourcePath)
  return mod.exports.default
}

test('new dashboard configuration labels are localized in built-in locales', async () => {
  for (const lang of ['zh-CN', 'en', 'tw']) {
    const locale = loadLocale(lang)
    for (const key of requiredKeys) {
      const value = getValue(locale, key)
      assert.equal(typeof value, 'string', `${lang} missing ${key}`)
      assert.notEqual(value.trim(), '', `${lang} has empty ${key}`)
      assert.notEqual(value, key, `${lang} leaks raw key ${key}`)
    }
  }
})
