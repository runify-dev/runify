# Runify

**轻量级白盒 AI Agent 平台** — 低资源占用，可视化构建、完全可控的自定义 AI Agent。

你看到的每一步推理、每一次工具调用、每一个决策节点，都是透明的、可编辑的、可调试的。不黑盒，不魔法，一切尽在掌握。

## 轻量运行

Runify 基于 Vert.x 高性能异步框架，启动快、占用少，一台普通服务器甚至个人电脑就能跑起来，无需昂贵的集群资源。

## 核心能力

### 自定义 AI Agent

通过可视化工作流编辑器，拖拽节点即可构建专属 Agent：

| 节点类型    | 说明 |
|---------|---|
| AI 对话   | 多模型接入，支持思维链、工具调用 |
| 条件判断    | 根据变量 / AI 输出走不同分支 |
| 循环      | 批量处理、迭代优化 |
| 数据库查询   | 直连数据源，Agent 自主检索 |
| 笔记检索    | Agent 可检索笔记内容，精准召回 |
| 代码执行    | JavaScript / 终端，Agent 可编程 |
| 文件操作    | 读写文件、上传下载 |
| 变量赋值    | 流程状态管理 |
| 缓存读写    | 避免重复计算 |
| HTTP 请求 | 工作流驱动，调用外部 API / Webhook |
| 审批节点    | 人机协作，关键步骤人工确认 |

### 智能笔记

富文本编辑器，支持 Markdown、代码高亮、数学公式、图片。笔记内容可被 Agent 检索和引用。

### 多数据源

PostgreSQL、SQLite、H2、MySQL — Agent 可直接查询你的业务数据。

### 多端适配

前端响应式设计，PC 和手机端自适应，随时随地管理你的 Agent 和笔记。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 25 · Vert.x · Dagger · Flyway |
| 前端 | Vue 3 · TypeScript · Vite · PrimeVue · Tailwind CSS |
| 工作流 | 自研引擎 · LogicFlow 画布 |
| 搜索 | Lucene / Elasticsearch |
| 桌面 | Electron (macOS / Windows) |

## 快速开始

### 环境要求

- JDK 25+
- Node.js 20+
- Maven 3.8+

### 开发模式

```bash
# 启动后端
mvn clean package -DskipTests
java -jar backend/target/backend.jar

# 启动前端（另一个终端）
cd frontend
npm install
npm run dev
```

### 构建部署

```bash
# JAR 包
./installer/build-jar.sh
java -jar release/runify.jar

# 桌面应用
./installer/installer.sh [mac|win]
```

### macOS 安装

首次打开 macOS 安装包时，系统提示"无法打开，因为无法验证开发者"，需要执行：

```bash
xattr -cr /Applications/Runify.app
```

移除隔离属性后即可正常启动。

## 项目结构

```
run/
├── backend/
│   └── src/main/java/com/run/
│       ├── workflow/     # 工作流引擎（Agent 核心）
│       │   ├── nodes/    # 节点实现
│       │   └── entity/   # 流程定义
│       ├── ai/           # AI 模型接入
│       ├── route/        # API 路由
│       ├── handler/      # 业务处理
│       ├── dao/          # 数据访问
│       └── datasources/  # 多数据源适配
├── frontend/
│   └── src/
│       ├── workflow/     # 工作流可视化编辑器
│       ├── views/        # 页面
│       ├── editor/       # 富文本编辑器
│       └── locales/      # 国际化
├── installer/
│   ├── electron/         # 桌面端
│   ├── build-jar.sh
│   └── installer.sh
└── pom.xml
```

## 技术交流

<img width="930" height="1446" alt="c177135ae214abf9370e57496da28f76" src="https://github.com/user-attachments/assets/06fc662a-38bd-40e8-9e7d-0a020f154093" />


## 参与讨论

我们欢迎所有人对 Runify 提出看法、建议和想法，不只是代码贡献。

- **[Discussions](../../discussions)** — 分享你的想法、提出建议、讨论功能需求
- **[Issues](../../issues)** — 提交 Bug 报告、功能请求
- **[Pull Requests](../../pulls)** — 代码贡献

无论你是开发者、设计师、产品经理还是普通用户，你的意见都对我们很重要。

## License

[Apache License 2.0](LICENSE)

所有贡献者需签署 [CLA](CLA.md)。
