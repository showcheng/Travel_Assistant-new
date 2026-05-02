import { marked } from 'marked'
import DOMPurify from 'dompurify'

/**
 * Composable for rendering Markdown text to sanitized HTML.
 * Uses `marked` for parsing and `DOMPurify` for XSS prevention.
 */
export function useMarkdown() {
  /**
   * Render a Markdown string to sanitized HTML.
   *
   * @param text - Raw Markdown input
   * @returns Sanitized HTML string
   */
  const render = (text: string): string => {
    if (!text) return ''

    const html = marked.parse(text, { breaks: true }) as string

    return DOMPurify.sanitize(html, {
      ALLOWED_TAGS: [
        'p', 'br', 'strong', 'em', 'ul', 'ol', 'li',
        'a', 'code', 'pre', 'h1', 'h2', 'h3', 'h4',
        'blockquote', 'table', 'thead', 'tbody', 'tr',
        'th', 'td'
      ],
      ALLOWED_ATTR: ['href', 'target', 'class']
    })
  }

  return { render }
}
