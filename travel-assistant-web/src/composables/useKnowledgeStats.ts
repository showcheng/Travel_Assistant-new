/**
 * Composable for computing knowledge base statistics used in dashboard charts.
 *
 * Provides pure functions for deriving category distributions, search trend
 * data, and aggregate totals from document arrays.
 */

interface CategoryDocument {
  category: string
  id: string
}

interface TotalsDocument {
  id: string
  chunkCount?: number
  category: string
}

interface CategoryDistributionEntry {
  name: string
  value: number
}

interface TrendEntry {
  date: string
  count: number
}

interface Totals {
  docCount: number
  chunkCount: number
  categoryCount: number
}

export function useKnowledgeStats() {
  /**
   * Compute category distribution from a list of documents.
   * Returns entries sorted by count descending.
   */
  const getCategoryDistribution = (
    docs: CategoryDocument[]
  ): CategoryDistributionEntry[] => {
    const map = new Map<string, number>()
    for (const doc of docs) {
      const current = map.get(doc.category) ?? 0
      map.set(doc.category, current + 1)
    }
    return Array.from(map.entries())
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value)
  }

  /**
   * Generate 7-day mock trend data for the search trend line chart.
   * Each entry contains a date string (M/D format) and a random count.
   */
  const getSearchTrend = (): TrendEntry[] => {
    const trend: TrendEntry[] = []
    const today = new Date()
    for (let i = 6; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)
      trend.push({
        date: `${date.getMonth() + 1}/${date.getDate()}`,
        count: Math.floor(Math.random() * 20) + 5,
      })
    }
    return trend
  }

  /**
   * Compute aggregate totals from a list of documents.
   */
  const getTotals = (docs: TotalsDocument[]): Totals => {
    const categories = new Set(docs.map((d) => d.category))
    return {
      docCount: docs.length,
      chunkCount: docs.reduce((sum, d) => sum + (d.chunkCount ?? 0), 0),
      categoryCount: categories.size,
    }
  }

  return { getCategoryDistribution, getSearchTrend, getTotals }
}
