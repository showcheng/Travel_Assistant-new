import { describe, it, expect } from 'vitest'

/**
 * Tests for chart configuration logic used in KnowledgeBaseView.
 *
 * The pie chart and line chart option builders are exercised through
 * the useKnowledgeStats composable (tested separately) and the
 * buildPieChartOption / buildLineChartOption helper functions that
 * transform composable output into ECharts option objects.
 */

describe('KnowledgeBaseView charts', () => {
  // ---------------------------------------------------------------------------
  // Category display name mapping
  // ---------------------------------------------------------------------------
  describe('getCategoryDisplayName', () => {
    it('should map known category codes to Chinese display names', async () => {
      const { getCategoryDisplayName } = await import('../KnowledgeBaseChartHelpers')
      expect(getCategoryDisplayName('ATTRACTION')).toBe('景点介绍')
      expect(getCategoryDisplayName('POLICY')).toBe('政策说明')
      expect(getCategoryDisplayName('ROUTE')).toBe('路线推荐')
      expect(getCategoryDisplayName('FAQ')).toBe('常见问题')
    })

    it('should return the raw code for unknown categories', async () => {
      const { getCategoryDisplayName } = await import('../KnowledgeBaseChartHelpers')
      expect(getCategoryDisplayName('UNKNOWN')).toBe('UNKNOWN')
      expect(getCategoryDisplayName('')).toBe('')
    })
  })

  // ---------------------------------------------------------------------------
  // Pie chart option builder
  // ---------------------------------------------------------------------------
  describe('buildPieChartOption', () => {
    it('should produce a valid ECharts pie option with named categories', async () => {
      const { buildPieChartOption } = await import('../KnowledgeBaseChartHelpers')
      const data = [
        { name: '景点介绍', value: 3 },
        { name: '政策说明', value: 2 },
        { name: '路线推荐', value: 1 },
      ]

      const option = buildPieChartOption(data)

      expect(option.tooltip.trigger).toBe('item')
      expect(option.series).toHaveLength(1)
      expect(option.series[0].type).toBe('pie')
      expect(option.series[0].data).toEqual(data)
      expect(option.series[0].radius).toEqual(['40%', '70%'])
    })

    it('should show placeholder when data is empty', async () => {
      const { buildPieChartOption } = await import('../KnowledgeBaseChartHelpers')
      const option = buildPieChartOption([])

      expect(option.series[0].data).toEqual([{ name: '暂无数据', value: 1 }])
    })

    it('should include a legend with vertical orientation', async () => {
      const { buildPieChartOption } = await import('../KnowledgeBaseChartHelpers')
      const option = buildPieChartOption([{ name: 'A', value: 1 }])

      expect(option.legend.orient).toBe('vertical')
      expect(option.legend.left).toBe('left')
    })

    it('should define custom color palette', async () => {
      const { buildPieChartOption } = await import('../KnowledgeBaseChartHelpers')
      const option = buildPieChartOption([{ name: 'A', value: 1 }])

      expect(Array.isArray(option.color)).toBe(true)
      expect(option.color.length).toBeGreaterThanOrEqual(4)
    })
  })

  // ---------------------------------------------------------------------------
  // Line chart option builder
  // ---------------------------------------------------------------------------
  describe('buildLineChartOption', () => {
    it('should produce a valid ECharts line option with date x-axis', async () => {
      const { buildLineChartOption } = await import('../KnowledgeBaseChartHelpers')
      const trend = [
        { date: '4/26', count: 12 },
        { date: '4/27', count: 8 },
        { date: '4/28', count: 15 },
      ]

      const option = buildLineChartOption(trend)

      expect(option.tooltip.trigger).toBe('axis')
      expect(option.xAxis.type).toBe('category')
      expect(option.xAxis.data).toEqual(['4/26', '4/27', '4/28'])
      expect(option.yAxis.type).toBe('value')
      expect(option.series).toHaveLength(1)
      expect(option.series[0].type).toBe('line')
      expect(option.series[0].data).toEqual([12, 8, 15])
    })

    it('should configure smooth line with area style', async () => {
      const { buildLineChartOption } = await import('../KnowledgeBaseChartHelpers')
      const trend = [{ date: '1/1', count: 5 }]
      const option = buildLineChartOption(trend)

      expect(option.series[0].smooth).toBe(true)
      expect(option.series[0].areaStyle).toBeDefined()
      expect(option.series[0].lineStyle.color).toBe('#409EFF')
      expect(option.series[0].itemStyle.color).toBe('#409EFF')
    })

    it('should handle empty trend data gracefully', async () => {
      const { buildLineChartOption } = await import('../KnowledgeBaseChartHelpers')
      const option = buildLineChartOption([])

      expect(option.xAxis.data).toEqual([])
      expect(option.series[0].data).toEqual([])
    })

    it('should set grid margins for readability', async () => {
      const { buildLineChartOption } = await import('../KnowledgeBaseChartHelpers')
      const option = buildLineChartOption([{ date: '1/1', count: 1 }])

      expect(option.grid).toBeDefined()
      expect(option.grid.left).toBeDefined()
      expect(option.grid.right).toBeDefined()
    })
  })

  // ---------------------------------------------------------------------------
  // Integration: stats composable feeds chart builders
  // ---------------------------------------------------------------------------
  describe('integration: stats to chart option pipeline', () => {
    it('should build pie option from getCategoryDistribution output', async () => {
      const { useKnowledgeStats } = await import('@/composables/useKnowledgeStats')
      const { buildPieChartOption, getCategoryDisplayName } = await import('../KnowledgeBaseChartHelpers')

      const { getCategoryDistribution } = useKnowledgeStats()
      const docs = [
        { category: 'ATTRACTION', id: '1' },
        { category: 'ATTRACTION', id: '2' },
        { category: 'POLICY', id: '3' },
      ]

      const distribution = getCategoryDistribution(docs).map((entry) => ({
        name: getCategoryDisplayName(entry.name),
        value: entry.value,
      }))
      const option = buildPieChartOption(distribution)

      expect(option.series[0].data).toEqual([
        { name: '景点介绍', value: 2 },
        { name: '政策说明', value: 1 },
      ])
    })

    it('should build line option from getSearchTrend output', async () => {
      const { useKnowledgeStats } = await import('@/composables/useKnowledgeStats')
      const { buildLineChartOption } = await import('../KnowledgeBaseChartHelpers')

      const { getSearchTrend } = useKnowledgeStats()
      const trend = getSearchTrend()
      const option = buildLineChartOption(trend)

      expect(option.xAxis.data).toHaveLength(7)
      expect(option.series[0].data).toHaveLength(7)
    })
  })
})
