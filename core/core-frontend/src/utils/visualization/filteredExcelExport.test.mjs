import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  EXCEL_EXPORT_SCOPE,
  buildExportChartExtRequest,
  collectExportableTableViews,
  defaultExportButtonConfig,
  validateExportTarget,
  validateQueryComponentsForExport
} from './filteredExcelExport.mjs'

describe('filtered excel export helpers', () => {
  it('collects exportable table views from nested canvas components', () => {
    const components = [
      {
        component: 'Group',
        propValue: [
          { component: 'UserView', id: 'chart-1' },
          {
            component: 'Tabs',
            propValue: [{ componentData: [{ component: 'UserView', id: 'chart-2' }] }]
          }
        ]
      },
      { component: 'UserView', id: 'chart-3' }
    ]
    const views = {
      'chart-1': { type: 'bar', title: '柱状图' },
      'chart-2': { type: 'table-info', title: '明细表' },
      'chart-3': { type: 'table-normal', title: '汇总表' }
    }

    assert.deepEqual(
      collectExportableTableViews(components, views).map(item => item.label),
      ['明细表', '汇总表']
    )
  })

  it('validates required query controls before exporting filtered data', () => {
    const components = [
      {
        component: 'VQuery',
        id: 'query-1',
        propValue: [
          {
            id: 'filter-1',
            name: '客户',
            required: true,
            displayType: '0',
            selectValue: '',
            checkedFields: ['chart-1'],
            checkedFieldsMap: {
              'chart-1': 'field-1'
            }
          }
        ]
      }
    ]

    assert.deepEqual(validateQueryComponentsForExport(components, 'chart-1'), {
      valid: false,
      reason: 'required',
      queryName: '客户',
      componentId: 'query-1'
    })
  })

  it('validates required query controls inside chart groups', () => {
    const components = [
      {
        component: 'Group',
        propValue: [
          {
            component: 'VQuery',
            id: 'query-in-group',
            propValue: [
              {
                id: 'filter-in-group',
                name: '区域',
                required: true,
                displayType: '0',
                selectValue: '',
                checkedFields: ['chart-1'],
                checkedFieldsMap: {
                  'chart-1': 'field-1'
                }
              }
            ]
          },
          { component: 'UserView', id: 'chart-1' }
        ]
      }
    ]

    assert.deepEqual(validateQueryComponentsForExport(components, 'chart-1'), {
      valid: false,
      reason: 'required',
      queryName: '区域',
      componentId: 'query-in-group'
    })
  })

  it('validates incomplete and reversed number ranges', () => {
    const components = [
      {
        component: 'VQuery',
        id: 'query-1',
        propValue: [
          {
            id: 'filter-1',
            name: '金额',
            displayType: '22',
            numValueStart: 100,
            numValueEnd: '',
            checkedFields: ['chart-1'],
            checkedFieldsMap: {
              'chart-1': 'field-1'
            }
          },
          {
            id: 'filter-2',
            name: '数量',
            displayType: '22',
            numValueStart: 10,
            numValueEnd: 2,
            checkedFields: ['chart-2'],
            checkedFieldsMap: {
              'chart-2': 'field-2'
            }
          }
        ]
      }
    ]

    assert.equal(validateQueryComponentsForExport(components, 'chart-1').reason, 'numberRangeIncomplete')
    assert.equal(validateQueryComponentsForExport(components, 'chart-2').reason, 'numberRangeInvalid')
  })

  it('builds current filtered and all-data chart requests differently', () => {
    const lastRequest = {
      user: 1,
      filter: [{ fieldId: 'f1', value: ['A'] }],
      linkageFilters: [{ fieldId: 'f2', value: ['B'] }],
      outerParamsFilters: [{ fieldId: 'f3', value: ['C'] }],
      webParamsFilters: [{ fieldId: 'f4', value: ['D'] }],
      drill: [{ dimensionList: [] }],
      resultCount: 20,
      resultMode: 'custom'
    }
    const viewInfo = { resultCount: 1000, resultMode: 'all' }

    assert.equal(
      buildExportChartExtRequest({
        lastRequest,
        viewInfo,
        scope: EXCEL_EXPORT_SCOPE.CURRENT_FILTERED
      }).filter.length,
      1
    )
    assert.deepEqual(
      buildExportChartExtRequest({
        lastRequest,
        viewInfo,
        scope: EXCEL_EXPORT_SCOPE.ALL
      }),
      {
        user: 1,
        filter: [],
        linkageFilters: [],
        outerParamsFilters: [],
        webParamsFilters: [],
        drill: [],
        resultCount: 1000,
        resultMode: 'all'
      }
    )
  })

  it('validates export target and normalizes button defaults', () => {
    assert.deepEqual(defaultExportButtonConfig({ text: '下载' }), {
      targetViewId: '',
      scope: EXCEL_EXPORT_SCOPE.CURRENT_FILTERED,
      content: 'view',
      text: '下载'
    })

    assert.equal(
      validateExportTarget({
        targetViewId: 'chart-1',
        components: [],
        canvasViewInfo: { 'chart-1': { type: 'bar' } }
      }).reason,
      'unsupportedViewType'
    )
  })
})
