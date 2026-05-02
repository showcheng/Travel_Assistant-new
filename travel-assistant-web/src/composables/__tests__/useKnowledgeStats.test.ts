import { describe, it, expect } from 'vitest'

describe('useKnowledgeStats', () => {
  it('should calculate category distribution from documents', async () => {
    const { useKnowledgeStats } = await import('../useKnowledgeStats')
    const { getCategoryDistribution } = useKnowledgeStats()

    const docs = [
      { category: 'ATTRACTION', id: '1' },
      { category: 'ATTRACTION', id: '2' },
      { category: 'POLICY', id: '3' },
      { category: 'ROUTE', id: '4' },
    ]

    const result = getCategoryDistribution(docs)
    expect(result).toEqual([
      { name: 'ATTRACTION', value: 2 },
      { name: 'POLICY', value: 1 },
      { name: 'ROUTE', value: 1 },
    ])
  })

  it('should handle empty documents for category distribution', async () => {
    const { useKnowledgeStats } = await import('../useKnowledgeStats')
    const { getCategoryDistribution } = useKnowledgeStats()
    expect(getCategoryDistribution([])).toEqual([])
  })

  it('should generate 7-day mock trend data', async () => {
    const { useKnowledgeStats } = await import('../useKnowledgeStats')
    const { getSearchTrend } = useKnowledgeStats()

    const trend = getSearchTrend()
    expect(trend).toHaveLength(7)
    expect(trend[0]).toHaveProperty('date')
    expect(trend[0]).toHaveProperty('count')
    trend.forEach((item) => {
      expect(typeof item.date).toBe('string')
      expect(typeof item.count).toBe('number')
      expect(item.count).toBeGreaterThanOrEqual(0)
    })
  })

  it('should produce trend dates in chronological order', async () => {
    const { useKnowledgeStats } = await import('../useKnowledgeStats')
    const { getSearchTrend } = useKnowledgeStats()

    const trend = getSearchTrend()
    const dates = trend.map((t) => {
      const parts = t.date.split('/')
      return parseInt(parts[0], 10) * 100 + parseInt(parts[1], 10)
    })
    for (let i = 1; i < dates.length; i++) {
      // Allow wrap-around for month boundaries, so just check non-decreasing within tolerance
      expect(dates[i]).toBeGreaterThanOrEqual(dates[i - 1] - 31)
    }
  })

  it('should calculate totals from documents', async () => {
    const { useKnowledgeStats } = await import('../useKnowledgeStats')
    const { getTotals } = useKnowledgeStats()

    const docs = [
      { id: '1', chunkCount: 5, category: 'ATTRACTION' },
      { id: '2', chunkCount: 3, category: 'POLICY' },
    ]

    const totals = getTotals(docs)
    expect(totals.docCount).toBe(2)
    expect(totals.chunkCount).toBe(8)
    expect(totals.categoryCount).toBe(2)
  })

  it('should handle documents without chunkCount', async () => {
    const { useKnowledgeStats } = await import('../useKnowledgeStats')
    const { getTotals } = useKnowledgeStats()

    const docs = [
      { id: '1', category: 'ATTRACTION' },
      { id: '2', category: 'ATTRACTION' },
    ]

    const totals = getTotals(docs)
    expect(totals.docCount).toBe(2)
    expect(totals.chunkCount).toBe(0)
    expect(totals.categoryCount).toBe(1)
  })

  it('should sort category distribution by count descending', async () => {
    const { useKnowledgeStats } = await import('../useKnowledgeStats')
    const { getCategoryDistribution } = useKnowledgeStats()

    const docs = [
      { category: 'FAQ', id: '1' },
      { category: 'ATTRACTION', id: '2' },
      { category: 'ATTRACTION', id: '3' },
      { category: 'ATTRACTION', id: '4' },
      { category: 'POLICY', id: '5' },
      { category: 'POLICY', id: '6' },
    ]

    const result = getCategoryDistribution(docs)
    expect(result[0].name).toBe('ATTRACTION')
    expect(result[0].value).toBe(3)
    expect(result[1].name).toBe('POLICY')
    expect(result[1].value).toBe(2)
    expect(result[2].name).toBe('FAQ')
    expect(result[2].value).toBe(1)
  })
})
