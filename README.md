<p align="center">
  <h1 align="center">Runify</h1>
  <p align="center">
    <strong>轻量白盒多智能体平台</strong><br/>
    透明可控 · 可视化编排 · 高精度知识召回 · 企业级权限 · 低成本嵌入第三方 · 桌面安装开箱即用 · 200MB 即可运行
  </p>
</p>

<p align="center">
  <a href="#-快速开始">快速开始</a> · 
  <a href="#-为什么选择-runify">为什么选择 Runify</a> · 
  <a href="#-核心能力">核心能力</a> · 
  <a href="#-技术栈">技术栈</a> · 
  <a href="#-项目结构">项目结构</a> · 
  <a href="#-生产部署">生产部署</a> · 
  <a href="#-参与贡献">参与贡献</a>
</p>

<p align="center">
  <a href="https://github.com/runify-dev/runify/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License" /></a>
  <a href="https://github.com/runify-dev/runify/releases"><img src="https://img.shields.io/github/v/release/runify-dev/runify?include_prereleases" alt="Release" /></a>
  <a href="https://github.com/runify-dev/runify/stargazers"><img src="https://img.shields.io/github/stars/runify-dev/runify" alt="Stars" /></a>
</p>

<p align="center">
  <a href="https://github.com/runify-dev/runify/releases/download/v0.2.0-beta/Runify-1.0.0-arm64.dmg">📦 下载 macOS (Apple Silicon)</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a href="https://github.com/runify-dev/runify/releases/download/v0.2.0-beta/Runify.Setup.1.0.0.exe">📦 下载 Windows</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a href="https://github.com/runify-dev/runify/releases/download/v0.2.0-beta/runify.jar">☕ 下载 JAR 包</a>
</p>

🌐 **完整介绍与在线了解 → [runify.cn](https://runify.cn)**
---

## 🎬 项目演示
<!-- 建议替换为你的截图或 GIF -->

| <img width="400" src="https://github.com/user-attachments/assets/2ea7088b-a1a1-45c4-a235-bc19bcde2b8a" /> |  <img width="400" src="https://github.com/user-attachments/assets/565a2868-5c7d-439f-9993-d4a01cb95d9b" /> |
|:---:|:---:|
| <img width="400" src="https://github.com/user-attachments/assets/e0f92360-8ff5-483e-99ee-a846b8905714" />|  <img width="400" src="https://github.com/user-attachments/assets/28a3fe5e-dcf3-4e2d-96b2-217420cfb81d" />  |
|<video  width="400" src="https://github.com/user-attachments/assets/bc344b35-4fd6-434b-b81b-4ea861ea0251">|<video  width="400" src="https://github.com/user-attachments/assets/dcbf292f-9fa5-432e-9cb2-a4978f7c9ff3">
|



---

## ✨ 核心能力

### 🎯 开箱即用，小白也能无脑上手

不需要你装 Docker，不需要你会写 YAML，不需要你碰命令行。Runify 提供 **macOS / Windows 桌面安装包**，双击安装、打开即用，像装一个普通桌面软件一样简单。

同时内置两套预制工作流模板，安装完成即可直接使用：

- **🤖 智能体工作流模板**：预配置好 AI 对话 + 工具调用 + 条件分支的标准 Agent 流程，改个提示词就能跑
- **📖 知识库检索工作流模板**：预配置好文档导入 → 知识切片 → 精准检索 → AI 回答的完整 RAG 链路，导入文档即出效果

> 💡 从安装到第一个 Agent 跑通，最快 **5 分钟**。

### 🧩 可视化多智能体工作流

通过拖拽式画布，像搭积木一样自由组合核心节点。基于 DAG（有向无环图）执行引擎，摒弃复杂的 BPMN 规范，更符合开发者直觉。

**支持的节点类型：**

| 节点类型 | 说明 |
|---------|------|
| AI 对话 | 多模型接入，支持思维链、工具调用 |
| 条件判断 | 根据变量 / AI 输出走不同分支 |
| 循环 | 批量处理、迭代优化 |
| 数据库查询 | 直连数据源，Agent 自主检索 |
| 笔记检索 | Agent 可检索笔记内容，精准召回 |
| 代码执行 | JavaScript / 终端，Agent 可编程 |
| 文件操作 | 读写文件、上传下载 |
| 变量赋值 | 流程状态管理 |
| 缓存读写 | 避免重复计算 |
| HTTP 请求 | 调用外部 API / Webhook |
| 审批节点 | 人机协作，关键步骤人工确认 |

### 📚 高精度知识召回

集成现代化富文本编辑器，构建你的私有知识库。Agent 不再"瞎猜"，而是基于你的真实数据给出精准回答。

- **多要素渲染**：支持 Markdown、代码块高亮、数学公式及图表多媒体混排
- **无感自动保存**：实时暂存每一行输入，断网断电也不丢失
- **精准检索**：内置 Lucene 高精度检索引擎，无需额外搜索集群

### 🏢 企业级权限与对话管理

不止是一个开发者工具，Runify 为团队和企业场景做好了准备：

- **RBAC 权限控制**：细粒度的用户角色与权限管理，满足企业合规需求
- **统一对话管理**：多 Agent 对话统筹调度，对话记录可追溯、可审计
- **多数据源接入**：支持 PostgreSQL、MySQL、Redis，一键连接业务数据库

### 🔌 低成本嵌入第三方

Runify 天然为集成而生。通过 API 与 Webhook 即可快速嵌入现有业务系统，无需大规模改造，降低企业 AI 落地门槛。

### 📱 多端适配 & 桌面级安装体验

- **桌面端（Electron）**：提供 macOS / Windows 安装包，双击安装，无需命令行、不依赖 Docker，真正的桌面级应用体验
- **Web 响应式**：Tailwind CSS 原子化样式，手机端也能流畅处理 Agent 审批

---

## 🛠 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 25 · Vert.x · Dagger · Flyway |
| 前端 | Vue 3 · TypeScript · Vite · PrimeVue · Tailwind CSS |
| 工作流 | 自研 DAG 引擎 · LogicFlow 画布 |
| 搜索 | Lucene（内置） / Elasticsearch（可选） |
| 桌面 | Electron（macOS / Windows） |

### ⚡ 轻量级 ≠ 低性能

很多轻量框架在并发面前不堪一击，Runify 不是。底层基于 **Vert.x 异步非阻塞（NIO）架构**，仅 200MB 内存即可运行，同时用 Event Loop 驱动 I/O，而非传统的一请求一线程模型：

- **异步全链路**：从 HTTP 接入、工作流调度到数据库查询，全程非阻塞，不会因单个慢请求拖垮整体
- **高吞吐低延迟**：少量线程即可处理大量并发连接，内存和 CPU 占用远低于同等负载下的传统方案
- **生产级稳定**：最小化外部依赖，核心组件高度内聚，减少级联故障风险，适合长时间无人值守运行
- **弹性扩展**：单实例性能不够时，可水平部署多个 JAR 实例，配合 Nginx 实现负载均衡

---

## 🚀 快速开始

### 方式一：桌面安装（推荐，最简单）

| 平台 | 下载 | 大小 |
|------|------|------|
| macOS (Apple Silicon) | [📦 Runify-1.0.0-arm64.dmg](https://github.com/runify-dev/runify/releases/download/v0.2.0-beta/Runify-1.0.0-arm64.dmg) | 412 MB |
| Windows | [📦 Runify Setup 1.0.0.exe](https://github.com/runify-dev/runify/releases/download/v0.2.0-beta/Runify.Setup.1.0.0.exe) | 371 MB |
| 跨平台 JAR | [☕ runify.jar](https://github.com/runify-dev/runify/releases/download/v0.2.0-beta/runify.jar) | 119 MB |

安装完成后打开 Runify，内置的工作流模板即刻可用，无需任何额外配置。不依赖 Docker。

### 方式二：源码开发

#### 环境要求

- JDK 25+
- Node.js 20+
- Maven 3.8+

#### 开发模式

```bash
# 启动后端
mvn clean package -DskipTests
java -jar backend/target/backend.jar

# 启动前端（另一个终端）
cd frontend
npm install
npm run dev
```

#### 构建部署

```bash
# JAR 包
./installer/build-jar.sh
java -jar release/runify.jar

# 桌面应用
./installer/installer.sh [mac|win]
```

#### macOS 安装说明

首次打开 macOS 安装包时，系统可能提示"无法验证开发者"，执行以下命令即可：

```bash
xattr -cr /Applications/Runify.app
```

默认账号：

```
用户名: admin
密码:   Runify@1
```

---

## 📂 项目结构

```
runify/
├── backend/
│   └── src/main/java/com/run/
│       ├── workflow/        # 工作流引擎（Agent 核心）
│       │   ├── nodes/       # 节点实现
│       │   └── entity/      # 流程定义
│       ├── ai/              # AI 模型接入
│       ├── route/           # API 路由
│       ├── handler/         # 业务处理
│       ├── dao/             # 数据访问
│       └── datasources/     # 多数据源适配
├── frontend/
│   └── src/
│       ├── workflow/        # 工作流可视化编辑器
│       ├── views/           # 页面
│       ├── editor/          # 富文本编辑器
│       └── locales/         # 国际化
├── installer/
│   ├── electron/            # 桌面端
│   ├── build-jar.sh
│   └── installer.sh
└── pom.xml
```

---
## 技术交流

<img width="300" height="300" alt="c177135ae214abf9370e57496da28f76" src="https://github.com/user-attachments/assets/b7639a91-89e5-46ec-934a-545cc747ac70" />

## 🤝 参与贡献

我们欢迎所有形式的参与，不只是代码贡献：

- **[Discussions](https://github.com/runify-dev/runify/discussions)** — 分享想法、提出建议、讨论功能需求
- **[Issues](https://github.com/runify-dev/runify/issues)** — 提交 Bug 报告、功能请求
- **[Pull Requests](https://github.com/runify-dev/runify/pulls)** — 代码贡献

无论你是开发者、设计师、产品经理还是普通用户，你的意见都对我们很重要。

---

## 📄 License

[Apache License 2.0](LICENSE)

所有贡献者需签署 [CLA](CLA.md)。
