<script setup lang="ts">
import { SpreadSheet, Node } from '@antv/s2'
import { PropType } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { S2Event, SortFuncParam } from '@antv/s2'
import { SortUp, SortDown, Sort } from '@element-plus/icons-vue'
import {
  getHeaderSortField,
  getHeaderSortStateId,
  sortTableRowsForHeader
} from '@/views/chart/components/js/panel/common/tableSortTooltip.mjs'

const { t } = useI18n()
// 定义组件入参，约束外部传入配置
const props = defineProps({
  table: {
    type: Object as PropType<SpreadSheet>,
    required: true
  },
  meta: {
    type: Object as PropType<Node>,
    required: true
  }
})
// 处理排序配置并同步展示顺序
const sortFunc = (sortParams: SortFuncParam) => {
  return sortTableRowsForHeader(sortParams)
}
// 处理排序配置并同步展示顺序
const sort = (type?) => {
  const sortFieldId = getHeaderSortField(props.meta)
  const sortStateId = getHeaderSortStateId(props.meta)
  props.table.updateSortMethodMap(sortStateId, type, true)
  props.table.emit(S2Event.RANGE_SORT, [
    {
      sortFieldId,
      sortMethod: type,
      sortFunc
    }
  ])
  props.table.hideTooltip()
}
</script>
<template>
  <el-col style="color: black">
    <el-row align="middle">
      <el-icon><SortUp /></el-icon>
      <span class="sort-btn" @click="() => sort('asc')">{{ t('chart.asc') }}</span>
    </el-row>
    <el-row align="middle">
      <el-icon><SortDown /></el-icon>
      <span class="sort-btn" @click="() => sort('desc')">{{ t('chart.desc') }}</span>
    </el-row>
    <el-row align="middle">
      <el-icon><Sort /></el-icon>
      <span class="sort-btn" @click="() => sort()">{{ t('chart.default') }}</span>
    </el-row>
  </el-col>
</template>
<style scoped>
.sort-btn {
  font-size: 14px;
  cursor: pointer;
}
</style>
