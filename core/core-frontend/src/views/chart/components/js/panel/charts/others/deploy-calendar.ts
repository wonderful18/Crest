import { AbstractChartView, ChartLibraryType, ChartRenderType } from '../../types'
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()
/**
 * 投产窗口日历图表
 * xAxis=投产日期列 yAxis=投产内容列 extStack=版本类型列(周版/月版)
 */
export class DeployCalendarView extends AbstractChartView {
  properties: EditorProperty[] = ['background-overall-component', 'border-style', 'linkage', 'jump-set']
  propertyInner: EditorPropertyInner = {
    'background-overall-component': ['all'],
    'border-style': ['all']
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
}
