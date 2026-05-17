# Club Management Frontend

基于 Vue 3 + Vite + TypeScript + Element Plus 的前端项目。

## 快速开始

```bash
npm install
npm run dev
```

- 开发服务器默认运行在 <http://localhost:5173>
- 通过 `.env.development` 中的 `VITE_API_BASE_URL` 配置后端 API 地址

## 主要技术栈

- [Vue 3](https://vuejs.org/) 组合式 API
- [Vite](https://vitejs.dev/) + TypeScript 开发体验
- [Pinia](https://pinia.vuejs.org/) 状态管理
- [Vue Router](https://router.vuejs.org/) 路由管理
- [Element Plus](https://element-plus.org/) 组件库
- [Axios](https://axios-http.com/) 网络请求

## 项目结构

```
web/
  ├── src/
  │   ├── api/            # API 请求封装
  │   ├── assets/         # 静态资源
  │   ├── components/     # 通用组件
  │   ├── layouts/        # 布局组件
  │   ├── router/         # 路由配置
  │   ├── store/          # Pinia 状态模块
  │   ├── styles/         # 全局样式
  │   ├── utils/          # 工具方法
  │   └── views/          # 页面
  ├── public/             # 静态文件
  └── ...
```

## 脚本命令

- `npm run dev` 启动开发服务器
- `npm run build` 生产构建
- `npm run preview` 预览构建结果
- `npm run lint` ESLint 校验
- `npm run format` Prettier 格式化
