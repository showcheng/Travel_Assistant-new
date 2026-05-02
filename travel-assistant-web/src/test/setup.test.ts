import { describe, it, expect } from 'vitest'

describe('Test Infrastructure', () => {
  it('should run vitest correctly', () => {
    expect(1 + 1).toBe(2)
  })

  it('should support TypeScript', () => {
    const greeting: string = 'hello'
    expect(greeting).toBeTypeOf('string')
  })
})
