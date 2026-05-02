/**
 * Pure helper functions for building ECharts configuration objects
 * used by KnowledgeBaseView chart panels.
 *
 * Extracted from the component so that option construction can be
 * unit-tested without a DOM or echarts runtime.
 */

interface ChartDataEntry {
  name: string
  value: number
}

interface TrendEntry {
  date: string
  count: number
}

interface PieChartOption {
  tooltip: { trigger: string; formatter: string }
  legend: { orient: string; left: string; top: string }
  series: Array<{
    type: string
    radius: string[]
    center: string[]
    avoidLabelOverlap: boolean
    data: ChartDataEntry[]
    emphasis: {
      itemStyle: {
        shadowBlur: number
        shadowOffsetX: number
        shadowColor: string
      }
    }
    label: { show: boolean; formatter: string }
  }>
  color: string[]
}

interface LineChartOption {
  tooltip: { trigger: string }
  grid: { left: string; right: string; bottom: string; top: string }
  xAxis: {
    type: string
    data: string[]
    boundaryGap: boolean
  }
  yAxis: { type: string; minInterval: number }
  series: Array<{
    data: number[]
    type: string
    smooth: boolean
    areaStyle: Record<string, unknown>
    lineStyle: { color: string; width: number }
    itemStyle: { color: string }
  }>
}

const CATEGORY_DISPLAY_NAMES: Record<string, string> = {
  ATTRACTION: '景点介绍',
  POLICY: '政策说明',
  ROUTE: '路线推荐',
  FAQ: '常见问题',
}

const CHART_COLORS = ['#67C23A', '#E6A23C', '#409EFF', '#F56C6C', '#909399']

/**
 * Map a category code to its Chinese display name.
 * Returns the raw code if no mapping exists.
 */
export function getCategoryDisplayName(code: string): string {
  return CATEGORY_DISPLAY_NAMES[code] ?? code
}

/**
 * Build an ECharts option object for the category distribution pie chart.
 * Shows a placeholder "暂无数据" entry when data is empty.
 */
export function buildPieChartOption(data: ChartDataEntry[]): PieChartOption {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'middle' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: true,
        data: data.length > 0 ? data : [{ name: '暂无数据', value: 1 }],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
        label: { show: true, formatter: '{b}: {c}' },
      },
    ],
    color: [...CHART_COLORS],
  }
}

/**
 * Build an ECharts option object for the 7-day search trend line chart.
 */
export function buildLineChartOption(trend: TrendEntry[]): LineChartOption {
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '10%', right: '5%', bottom: '15%', top: '10%' },
    xAxis: {
      type: 'category',
      data: trend.map((t) => t.date),
      boundaryGap: false,
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        data: trend.map((t) => t.count),
        type: 'line',
        smooth: true,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64,158,255,0.4)' },
              { offset: 1, color: 'rgba(64,158,255,0.05)' },
            ],
          },
        },
        lineStyle: { color: '#409EFF', width: 2 },
        itemStyle: { color: '#409EFF' },
      },
    ],
  }
}
