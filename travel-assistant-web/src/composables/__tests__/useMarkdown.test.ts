import { describe, it, expect } from 'vitest'

// These will fail until useMarkdown is created
describe('useMarkdown', () => {
  it('should render bold text', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    expect(render('**bold**')).toContain('<strong>bold</strong>')
  })

  it('should render italic text', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    expect(render('*italic*')).toContain('<em>italic</em>')
  })

  it('should render unordered lists', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    const result = render('- item1\n- item2')
    expect(result).toContain('<li>item1</li>')
    expect(result).toContain('<li>item2</li>')
  })

  it('should render ordered lists', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    const result = render('1. first\n2. second')
    expect(result).toContain('<li>first</li>')
    expect(result).toContain('<li>second</li>')
  })

  it('should render links', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    expect(render('[text](http://example.com)')).toContain('<a')
    expect(render('[text](http://example.com)')).toContain('href="http://example.com"')
  })

  it('should sanitize XSS - script tags', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    const result = render('<script>alert("xss")</script>')
    expect(result).not.toContain('<script')
    expect(result).not.toContain('alert')
  })

  it('should sanitize XSS - event handlers', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    const result = render('<img src=x onerror=alert(1)>')
    expect(result).not.toContain('onerror')
  })

  it('should handle newlines correctly', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    const result = render('line1\nline2')
    // Should produce separate lines (either <br> or <p> tags)
    expect(result).toContain('line1')
    expect(result).toContain('line2')
  })

  it('should return empty string for empty input', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    expect(render('')).toBe('')
  })

  it('should render code blocks', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    const result = render('```\ncode here\n```')
    expect(result).toContain('<code')
  })

  it('should render inline code', async () => {
    const { useMarkdown } = await import('../useMarkdown')
    const { render } = useMarkdown()
    expect(render('use `npm install` to install')).toContain('<code>npm install</code>')
  })
})
