# 智慧旅游助手 - 前端项目

基于 Vue 3 + TypeScript + Element Plus 的现代化旅游平台前端应用。

## 技术栈

- **框架**: Vue 3.4+ (Composition API)
- **语言**: TypeScript 5.3+
- **UI库**: Element Plus 2.5+
- **状态管理**: Pinia 2.1+
- **路由**: Vue Router 4.2+
- **构建工具**: Vite 5.0+
- **HTTP客户端**: Axios 1.6+

## 功能特性

### ✅ 已实现功能

1. **用户认证**
   - 手机号登录
   - 短信验证码注册
   - Token 认证
   - 路由守卫

2. **商品管理**
   - 商品列表展示（分页、筛选、搜索）
   - 商品详情查看
   - 商品图片预览
   - 价格和库存展示

3. **订单管理**
   - 创建订单
   - 订单列表查询
   - 订单状态筛选
   - 订单支付和取消

4. **个人中心**
   - 用户信息展示
   - 账户信息管理
   - 退出登录

### 🚧 开发中功能

- 购物车功能
- 秒杀商品专区
- 拼团功能
- AI 旅游规划助手
- 数字人直播

## 项目结构

```
travel-assistant-web/
├── src/
│   ├── api/              # API接口
│   │   ├── user.ts
│   │   ├── product.ts
│   │   └── order.ts
│   ├── assets/           # 静态资源
│   │   ├── css/
│   │   └── images/
│   ├── components/       # 公共组件
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # Pinia状态管理
│   │   └── user.ts
│   ├── types/            # TypeScript类型定义
│   │   └── index.ts
│   ├── utils/            # 工具函数
│   │   ├── index.ts
│   │   └── request.ts
│   ├── views/            # 页面组件
│   │   ├── LayoutView.vue
│   │   ├── LoginView.vue
│   │   ├── RegisterView.vue
│   │   ├── ProductListView.vue
│   │   ├── ProductDetailView.vue
│   │   ├── OrderListView.vue
│   │   ├── CreateOrderView.vue
│   │   ├── ProfileView.vue
│   │   └── NotFoundView.vue
│   ├── App.vue
│   └── main.ts
├── public/               # 公共文件
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## 快速开始

### 环境要求

- Node.js 18.0+
- npm 9.0+

### 安装依赖

```bash
cd travel-assistant-web
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 环境配置

### 开发环境 (.env.development)

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=智慧旅游助手
```

### 生产环境 (.env.production)

```env
VITE_API_BASE_URL=https://api.travel-assistant.com
VITE_APP_TITLE=智慧旅游助手
```

## API 对接

前端项目与后端 Spring Boot 服务通过 RESTful API 对接：

- **用户服务**: `/api/users/*`
- **商品服务**: `/api/products/*`
- **订单服务**: `/api/orders/*`

### 请求拦截

所有请求自动添加 Token 到请求头 `X-User-Id`。

### 响应拦截

- 业务错误码处理
- 统一错误提示
- 401 自动跳转登录

## 路由结构

- `/login` - 登录页
- `/register` - 注册页
- `/` - 主布局
  - `/products` - 商品列表
  - `/products/:id` - 商品详情
  - `/orders` - 订单列表
  - `/orders/create` - 创建订单
  - `/profile` - 个人中心
- `/*` - 404页面

## 开发规范

### 代码风格

- 使用 TypeScript 进行类型检查
- 遵循 ESLint 规范
- 使用 Composition API 编写组件

### 命名规范

- 组件文件：PascalCase (如 `ProductListView.vue`)
- 工具文件：camelCase (如 `formatDateTime`)
- 常量：UPPER_SNAKE_CASE

### 提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 重构代码
perf: 性能优化
test: 测试相关
chore: 构建/工具变动
```

## 性能优化

- 路由懒加载
- 图片懒加载
- 组件按需导入
- 请求防抖节流
- 响应式数据冻结

## 浏览器支持

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## 后续计划

1. **购物车功能**
   - 添加购物车
   - 购物车管理
   - 批量结算

2. **秒杀功能**
   - 秒杀商品列表
   - 倒计时展示
   - 防刷验证

3. **拼团功能**
   - 拼团商品展示
   - 发起/参与拼团
   - 拼团状态管理

4. **AI 功能**
   - AI 对话界面
   - 旅游规划展示
   - 智能推荐列表

5. **直播功能**
   - 直播视频播放
   - 弹幕互动
   - 商品卡片点击

## 许可证

MIT License
