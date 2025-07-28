# DiffGrader 前端设置指南

## 项目概述

DiffGrader 是一个智能代码比较和反馈评分系统，专为教学助理(TA)高效评估学生编程作业而设计。本前端使用 React + TypeScript 构建，提供现代化的用户界面。

## 功能特色

### 🚀 核心功能
- **📁 文件上传**: 支持拖拽上传 ZIP 文件，包含学生作业和参考解决方案
- **🔍 代码比较**: 使用 Monaco Editor 进行并排代码比较，智能高亮差异
- **📝 反馈系统**: 综合评分和反馈界面，支持设计模式和最佳实践评价
- **🎯 实时分析**: 代码分析过程中的实时状态更新
- **📱 响应式设计**: 适配桌面和移动设备

### 🛠️ 技术栈
- **React 18** + TypeScript
- **React Bootstrap** UI 组件库
- **Monaco Editor** 代码编辑器
- **Axios** HTTP 客户端
- **CSS3** 自定义样式

## 安装指南

### 前置要求
- Node.js 16 或更高版本
- npm 或 yarn 包管理器

### 快速开始

1. **进入前端目录**
   ```bash
   cd frontend
   ```

2. **安装依赖**
   ```bash
   npm install
   ```

3. **配置环境变量**
   ```bash
   # 创建环境配置文件
   # 编辑 .env 文件设置后端 API 地址
   ```

4. **启动开发服务器**
   ```bash
   npm start
   ```

应用将在 `http://localhost:3000` 启动。

### 环境配置

在 `frontend/.env` 文件中配置以下变量：

```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
REACT_APP_ENV=development
REACT_APP_VERSION=1.0.0
```

### 生产构建

```bash
npm run build
```

构建文件将生成到 `build` 目录。

## 项目结构

```
frontend/
├── public/                    # 静态文件
│   ├── index.html            # HTML 模板
│   └── favicon.ico           # 网站图标
├── src/
│   ├── components/           # React 组件
│   │   ├── FileUpload.tsx    # 文件上传组件
│   │   ├── CodeComparison.tsx # 代码比较组件
│   │   ├── FeedbackPanel.tsx  # 反馈面板组件
│   │   └── Header.tsx        # 头部导航组件
│   ├── services/             # API 服务
│   │   └── api.ts            # API 调用封装
│   ├── types/                # TypeScript 类型定义
│   │   └── index.ts          # 类型声明
│   ├── utils/                # 工具函数
│   │   └── helpers.ts        # 辅助函数
│   ├── App.tsx               # 主应用组件
│   ├── App.css               # 应用样式
│   └── index.tsx             # 应用入口点
├── package.json              # 项目配置
├── tsconfig.json             # TypeScript 配置
└── README.md                 # 项目文档
```

## 核心组件详解

### FileUpload 组件
- **功能**: 处理 ZIP 文件上传，支持拖拽操作
- **特性**: 文件验证、上传进度跟踪、支持学生和参考文件
- **验证**: 文件类型限制为 ZIP，最大 50MB

### CodeComparison 组件
- **功能**: 使用 Monaco Editor 进行并排代码比较
- **特性**: 元素导航(类、方法、字段、构造函数)、相似度高亮、差异检测
- **交互**: 点击元素查看详细比较，支持分类浏览

### FeedbackPanel 组件
- **功能**: 交互式评分界面
- **特性**: 评分滑块、评论字段、基于代码分析的自动建议
- **评价**: 支持设计模式和最佳实践反馈

### Header 组件
- **功能**: 应用导航头部
- **特性**: Logo、导航菜单、整体相似度显示

## API 集成

前端通过 RESTful API 与后端通信：

- `POST /api/files/upload` - 文件上传
- `POST /api/grading-sessions` - 创建评分会话
- `GET /api/grading-sessions/:id/comparison` - 获取比较结果
- `POST /api/grading-sessions/:id/feedback` - 提交反馈

## 使用流程

### 1. 上传文件
- 拖拽或选择学生作业 ZIP 文件
- 拖拽或选择参考解决方案 ZIP 文件
- 系统自动开始分析

### 2. 查看比较
- 浏览按类型组织的代码元素
- 点击任意元素查看并排比较
- 查看视觉指示器：
  - ✅ **完全匹配**
  - ≈ **相似元素**
  - ➖ **学生代码中缺失**
  - ➕ **学生代码中额外**

### 3. 提供反馈
- 查看代码差异
- 分配质量分数 (0-100)
- 添加关于设计模式和最佳实践的评论
- 保存反馈供学生查看

## 开发指南

### 代码规范
1. **组件结构**: 使用函数组件和 Hooks
2. **TypeScript**: 保持严格的类型安全
3. **错误处理**: 实现全面的错误边界
4. **性能**: 对昂贵组件使用 React.memo()
5. **可访问性**: 遵循 WCAG 指南

### 可用脚本
- `npm start` - 启动开发服务器
- `npm build` - 生产构建
- `npm test` - 运行测试
- `npm eject` - 退出 Create React App (不推荐)

## 故障排除

### 常见问题

1. **模块未找到错误**: 运行 `npm install`
2. **API 连接问题**: 检查后端服务器和 CORS 设置
3. **TypeScript 错误**: 确保所有依赖都已安装
4. **构建失败**: 清空 node_modules 并重新安装

### 调试模式

设置 `REACT_APP_ENV=development` 启用调试日志。

## 浏览器支持

- Chrome (最新版)
- Firefox (最新版)
- Safari (最新版)
- Edge (最新版)

## 性能优化

- React.lazy() 代码分割
- Monaco Editor 懒加载
- 优化打包大小
- 图片优化
- CSS 压缩

## 样式系统

应用使用以下样式技术：
- Bootstrap 5 基础组件
- 自定义 CSS 特定样式
- Font Awesome 图标
- 响应式设计原则

## 许可证

本项目采用 MIT 许可证。

---

## 联系信息

如果您在设置或使用过程中遇到问题，请参考文档或联系开发团队。 