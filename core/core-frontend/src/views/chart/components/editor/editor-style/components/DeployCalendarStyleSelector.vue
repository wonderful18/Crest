<script lang="ts" setup>
import { computed, onMounted, reactive, watch, nextTick, PropType } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import {
  COLOR_PANEL,
  DEFAULT_DEPLOY_CALENDAR_STYLE
} from '@/views/chart/components/editor/util/chart'
import { cloneDeep, defaultsDeep } from 'lodash-es'

const { t } = useI18n()

const props = defineProps({
  chart: {
    type: Object,
    required: true
  },
  themes: {
    type: String as PropType<EditorTheme>,
    default: 'dark'
  },
  propertyInner: {
    type: Array<string>
  }
})

const emit = defineEmits(['onDeployCalendarStyleChange'])

const predefineColors = COLOR_PANEL

const fontSizeList = computed(() => {
  const arr = []
  for (let i = 10; i <= 60; i = i + 2) {
    arr.push({ name: i + '', value: i })
  }
  return arr
})
const titleFontSizeList = computed(() => {
  const arr = []
  for (let i = 12; i <= 48; i = i + 2) {
    arr.push({ name: i + '', value: i })
  }
  return arr
})

const state = reactive({
  form: JSON.parse(JSON.stringify(DEFAULT_DEPLOY_CALENDAR_STYLE))
})

const change = (prop: string) => {
  emit('onDeployCalendarStyleChange', state.form, prop)
}

const init = () => {
  const custom = defaultsDeep(
    cloneDeep(props.chart?.customAttr?.deployCalendar),
    cloneDeep(DEFAULT_DEPLOY_CALENDAR_STYLE)
  )
  state.form = cloneDeep(custom)
  // 颜色初次赋值兜底
  nextTick(() => {
    state.form.titleColor = custom.titleColor
    state.form.dateColor = custom.dateColor
  })
}

onMounted(() => {
  init()
})

watch(
  () => props.chart?.customAttr?.deployCalendar,
  () => {
    init()
  },
  { deep: true }
)
</script>

<template>
  <div>
    <el-form
      :model="state.form"
      label-position="top"
      size="small"
      :effect="themes"
    >
      <!-- 月份标题 -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.title_color')"
      >
        <div style="display: flex">
          <el-color-picker
            :effect="themes"
            v-model="state.form.titleColor"
            class="color-picker-style"
            :predefine="predefineColors"
            @change="change('titleColor')"
            show-alpha
            is-custom
          />
          <el-tooltip :content="t('chart.font_size')" placement="top">
            <el-select
              style="width: 56px; margin-left: 8px"
              :effect="themes"
              v-model="state.form.titleFontSize"
              :placeholder="t('chart.text_fontsize')"
              size="small"
              @change="change('titleFontSize')"
            >
              <el-option
                v-for="option in titleFontSizeList"
                :key="option.value"
                :label="option.name"
                :value="option.value"
              />
            </el-select>
          </el-tooltip>
        </div>
      </el-form-item>
      <el-form-item class="form-item" :class="'form-item-' + themes" :effect="themes">
        <el-checkbox
          size="small"
          :effect="themes"
          v-model="state.form.titleIsBolder"
          @change="change('titleIsBolder')"
        >
          {{ t('deploy_calendar.title_bolder') }}
        </el-checkbox>
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 日期文字 -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.date_color')"
      >
        <div style="display: flex">
          <el-color-picker
            :effect="themes"
            v-model="state.form.dateColor"
            class="color-picker-style"
            :predefine="predefineColors"
            @change="change('dateColor')"
            show-alpha
            is-custom
          />
          <el-tooltip :content="t('chart.font_size')" placement="top">
            <el-select
              style="width: 56px; margin-left: 8px"
              :effect="themes"
              v-model="state.form.dateFontSize"
              :placeholder="t('chart.text_fontsize')"
              size="small"
              @change="change('dateFontSize')"
            >
              <el-option
                v-for="option in fontSizeList"
                :key="option.value"
                :label="option.name"
                :value="option.value"
              />
            </el-select>
          </el-tooltip>
        </div>
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 单元格底色 -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.cell_bg_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.cellBgColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('cellBgColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.cell_hover_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.cellHoverColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('cellHoverColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.deploy_cell_bg_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.deployCellBgColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('deployCellBgColor')"
          show-alpha
          is-custom
        />
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 状态高亮 -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.today_border_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.todayBorderColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('todayBorderColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.selected_border_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.selectedBorderColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('selectedBorderColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.selected_bg_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.selectedBgColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('selectedBgColor')"
          show-alpha
          is-custom
        />
      </el-form-item>

      <el-divider class="m-divider" :class="{ 'divider-dark': themes === 'dark' }" />

      <!-- 圆点与图例 -->
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.weekly_dot_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.weeklyDotColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('weeklyDotColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.monthly_dot_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.monthlyDotColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('monthlyDotColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.weekday_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.weekdayColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('weekdayColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
      <el-form-item
        class="form-item"
        :class="'form-item-' + themes"
        :effect="themes"
        :label="t('deploy_calendar.legend_color')"
      >
        <el-color-picker
          :effect="themes"
          v-model="state.form.legendColor"
          class="color-picker-style"
          :predefine="predefineColors"
          @change="change('legendColor')"
          show-alpha
          is-custom
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<style lang="less" scoped>
.form-item {
  margin-bottom: 12px;
}
:deep(.ed-color-picker.is-custom .ed-color-picker__mask) {
  height: 26px;
  width: 48px;
}
.m-divider {
  margin: 0 0 12px;
  border-color: rgba(31, 35, 41, 0.15);

  &.divider-dark {
    border-color: rgba(255, 255, 255, 0.15);
  }
}
</style>
