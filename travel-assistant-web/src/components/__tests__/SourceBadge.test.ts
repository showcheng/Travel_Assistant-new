import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'

// Stub that renders ElTag slot content so text assertions work
const ElTagStub = defineComponent({
  name: 'ElTag',
  props: ['type', 'size', 'effect'],
  setup(_, { slots }) {
    return () => h('span', { class: 'el-tag-stub' }, slots.default?.())
  }
})

// Stub that renders ElTooltip slot and exposes content prop as data attribute
const ElTooltipStub = defineComponent({
  name: 'ElTooltip',
  props: ['content'],
  setup(props, { slots }) {
    return () => h('span', { class: 'el-tooltip-stub', 'data-tooltip-content': props.content }, slots.default?.())
  }
})

const globalStubs = {
  ElTag: ElTagStub,
  ElTooltip: ElTooltipStub
}

describe('SourceBadge', () => {
  it('should render source title and relevance', async () => {
    const { default: SourceBadge } = await import('../SourceBadge.vue')
    const wrapper = mount(SourceBadge, {
      props: {
        sources: [
          { docTitle: '故宫博物院介绍', score: 0.85, category: '景点', relevance: '高度相关' }
        ]
      },
      global: { stubs: globalStubs }
    })
    expect(wrapper.text()).toContain('故宫博物院介绍')
    expect(wrapper.find('.source-badge').exists()).toBe(true)
  })

  it('should not render when sources is empty', async () => {
    const { default: SourceBadge } = await import('../SourceBadge.vue')
    const wrapper = mount(SourceBadge, {
      props: { sources: [] },
      global: { stubs: globalStubs }
    })
    expect(wrapper.find('.source-badge').exists()).toBe(false)
  })

  it('should render multiple sources', async () => {
    const { default: SourceBadge } = await import('../SourceBadge.vue')
    const wrapper = mount(SourceBadge, {
      props: {
        sources: [
          { docTitle: 'Source A', score: 0.9, category: '景点', relevance: '高度相关' },
          { docTitle: 'Source B', score: 0.5, category: '政策', relevance: '一般相关' }
        ]
      },
      global: { stubs: globalStubs }
    })
    expect(wrapper.text()).toContain('Source A')
    expect(wrapper.text()).toContain('Source B')
  })

  it('should not render when sources is undefined/null', async () => {
    const { default: SourceBadge } = await import('../SourceBadge.vue')
    const wrapper = mount(SourceBadge, {
      props: { sources: null as any },
      global: { stubs: globalStubs }
    })
    expect(wrapper.find('.source-badge').exists()).toBe(false)
  })

  it('should display score percentage in tooltip', async () => {
    const { default: SourceBadge } = await import('../SourceBadge.vue')
    const wrapper = mount(SourceBadge, {
      props: {
        sources: [
          { docTitle: 'Test Doc', score: 0.756, category: '景点', relevance: '一般相关' }
        ]
      },
      global: { stubs: globalStubs }
    })
    // Check that the tooltip stub received the correct content prop
    const tooltip = wrapper.find('.el-tooltip-stub')
    expect(tooltip.exists()).toBe(true)
    expect(tooltip.attributes('data-tooltip-content')).toContain('75.6')
  })
})
