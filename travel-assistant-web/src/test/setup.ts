import { vi } from 'vitest'
import { config } from '@vue/test-utils'

// Stub Element Plus components
config.global.stubs = {
  ElButton: true,
  ElInput: true,
  ElCard: true,
  ElTable: true,
  ElTag: true,
  ElDialog: true,
  ElForm: true,
  ElFormItem: true,
  ElSelect: true,
  ElOption: true,
  ElPagination: true,
  ElMessage: true,
  ElMessageBox: true,
  ElAvatar: true,
  ElTooltip: true,
  ElProgress: true,
  ElUpload: true,
  ElRadioGroup: true,
  ElRadioButton: true,
}

// Mock vue-router
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    go: vi.fn(),
    back: vi.fn(),
  }),
  useRoute: () => ({
    params: {},
    query: {},
    path: '/',
  }),
  RouterLink: true,
  RouterView: true,
}))
