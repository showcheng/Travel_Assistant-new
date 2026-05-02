import { ref } from 'vue'

/**
 * Composable for SSE (Server-Sent Events) streaming chat.
 * Connects to the backend SSE endpoint and processes streamed response chunks.
 */
export function useStreamChat() {
  const isStreaming = ref(false)

  /**
   * Stream a chat message via SSE and process response chunks.
   *
   * @param message - The user's message text
   * @param sessionId - The current conversation session ID
   * @param onChunk - Callback invoked for each text chunk received
   * @param onDone - Callback invoked when the stream completes successfully
   * @param onError - Callback invoked when an error occurs
   */
  async function streamChat(
    message: string,
    sessionId: string,
    onChunk: (text: string) => void,
    onDone: () => void,
    onError: (error: string) => void
  ): Promise<void> {
    isStreaming.value = true

    try {
      const params = new URLSearchParams({
        message,
        sessionId: sessionId || '',
        userId: '1'
      })

      const response = await fetch(`/api/ai/chat/stream?${params}`, {
        method: 'GET',
        headers: { 'Accept': 'text/event-stream' }
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('No readable stream')
      }

      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // Parse SSE events from buffer
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data === '[DONE]') {
              onDone()
            } else if (currentEvent === 'message') {
              onChunk(data)
            }
          } else if (line.trim() === '') {
            // Empty line resets event name per SSE spec
            currentEvent = ''
          }
        }
      }

      // If we reach here without explicit done event, still call onDone
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Stream failed'
      onError(errorMessage)
    } finally {
      isStreaming.value = false
    }
  }

  return { isStreaming, streamChat }
}
