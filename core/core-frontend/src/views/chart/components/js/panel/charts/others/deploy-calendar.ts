import { AbstractChartView, ChartLibraryType, ChartRenderType } from '../../types'
import { useI18n } from '@/hooks/web/useI18n'
import { defaultsDeep, cloneDeep } from 'lodash-es'
import { DEFAULT_DEPLOY_CALENDAR_STYLE } from '@/views/chart/components/editor/util/chart'

const { t } = useI18n()
/**
 * 投产窗口日历图表
 * xAxis=投产日期列 yAxis=关联系统列 extStack=版本类型列(周版/月版)
 */
export class DeployCalendarView extends AbstractChartView {
  properties: EditorProperty[] = [
    'title-selector',
    'deploy-calendar-style-selector',
    'background-overall-component',
    'border-style',
    'linkage',
    'jump-set'
  ]
  propertyInner: EditorPropertyInner = {
    'background-overall-component': ['all'],
    'border-style': ['all'],
    'deploy-calendar-style-selector': ['all']
  }
  selectorSpec: EditorSelectorSpec = {}
  axis: AxisType[] = ['xAxis', 'yAxis', 'extStack', 'filter']
  axisConfig: AxisConfig = {
    xAxis: {
      name: `${t('deploy_calendar.date_field')} / ${t('chart.dimension')}`,
      type: 'd',
      limit: 1
    },
    yAxis: {
      name: `${t('deploy_calendar.content_field')} / ${t('chart.dimension')}`,
      type: 'd',
      limit: 1
    },
    extStack: {
      name: `${t('deploy_calendar.release_type_field')} / ${t('chart.dimension')}`,
      type: 'd',
      limit: 1,
      allowEmpty: true
    }
  }
  constructor() {
    super(ChartRenderType.CUSTOM, ChartLibraryType.DEPLOY_CALENDAR, 'deploy-calendar')
  }
  /**
   * 新建/切换图表时补全日历样式默认值，兼容老数据（无 deployCalendar 字段）
   */
  setupDefaultOptions(chart: ChartObj): ChartObj {
    chart.customAttr = chart.customAttr || ({} as ChartObj['customAttr'])
    chart.customAttr.deployCalendar = defaultsDeep(
      cloneDeep(chart.customAttr.deployCalendar),
      cloneDeep(DEFAULT_DEPLOY_CALENDAR_STYLE)
    )
    return chart
  }
}
