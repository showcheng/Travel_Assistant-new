import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// RED phase: These tests define the contract for useStreamChat composable.
// The composable does not exist yet, so the import will fail.

describe('useStreamChat', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should start with isStreaming false', async () => {
    const { useStreamChat } = await import('../useStreamChat')
    const { isStreaming } = useStreamChat()
    expect(isStreaming.value).toBe(false)
  })

  it('should set isStreaming true during streaming and call onChunk for each data event', async () => {
    // Build mock SSE response
    const encoder = new TextEncoder()
    const chunks = [
      encoder.encode('event:message\ndata:你好世界\n\n'),
      encoder.encode('event:message\ndata:这是第二段\n\n'),
      encoder.encode('event:done\ndata:[DONE]\n\n')
    ]

    let readCallCount = 0
    const mockReader = {
      read: vi.fn().mockImplementation(() => {
        readCallCount++
        if (readCallCount <= chunks.length) {
          return Promise.resolve({ done: false, value: chunks[readCallCount - 1] })
        }
        return Promise.resolve({ done: true, value: undefined })
      })
    }

    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      body: { getReader: () => mockReader },
      status: 200
    })
    global.fetch = mockFetch

    const { useStreamChat } = await import('../useStreamChat')
    const { isStreaming, streamChat } = useStreamChat()

    const onChunk = vi.fn()
    const onDone = vi.fn()
    const onError = vi.fn()

    const streamPromise = streamChat('你好', 'session1', onChunk, onDone, onError)

    // While streaming, isStreaming should be true
    expect(isStreaming.value).toBe(true)

    await streamPromise

    // After streaming completes
    expect(onChunk).toHaveBeenCalledWith('你好世界')
    expect(onChunk).toHaveBeenCalledWith('这是第二段')
    expect(onDone).toHaveBeenCalled()
    expect(onError).not.toHaveBeenCalled()
    expect(isStreaming.value).toBe(false)
  })

  it('should call onError when fetch fails', async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error('Network error'))

    const { useStreamChat } = await import('../useStreamChat')
    const { streamChat, isStreaming } = useStreamChat()

    const onError = vi.fn()
    const onDone = vi.fn()
    const onChunk = vi.fn()

    await streamChat('test', 's1', onChunk, onDone, onError)

    expect(onError).toHaveBeenCalledWith('Network error')
    expect(onDone).not.toHaveBeenCalled()
    expect(isStreaming.value).toBe(false)
  })

  it('should call onError when fetch returns non-ok status', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      body: null
    })

    const { useStreamChat } = await import('../useStreamChat')
    const { streamChat } = useStreamChat()

    const onError = vi.fn()
    await streamChat('test', 's1', vi.fn(), vi.fn(), onError)

    expect(onError).toHaveBeenCalledWith('HTTP 500')
  })

  it('should call onError when response body has no reader', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      body: null
    })

    const { useStreamChat } = await import('../useStreamChat')
    const { streamChat } = useStreamChat()

    const onError = vi.fn()
    await streamChat('test', 's1', vi.fn(), vi.fn(), onError)

    expect(onError).toHaveBeenCalledWith('No readable stream')
  })

  it('should pass correct query parameters to fetch', async () => {
    const mockReader = {
      read: vi.fn()
        .mockResolvedValueOnce({ done: true, value: undefined })
    }

    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      body: { getReader: () => mockReader }
    })
    global.fetch = mockFetch

    const { useStreamChat } = await import('../useStreamChat')
    const { streamChat } = useStreamChat()

    await streamChat('hello world', 'sess-123', vi.fn(), vi.fn(), vi.fn())

    expect(mockFetch).toHaveBeenCalledTimes(1)
    const callUrl = mockFetch.mock.calls[0][0] as string
    expect(callUrl).toContain('/api/ai/chat/stream')
    // URLSearchParams encodes spaces as '+' which is equivalent to %20
    expect(callUrl).toContain('message=hello+world')
    expect(callUrl).toContain('sessionId=sess-123')
    expect(callUrl).toContain('userId=1')
  })
})
