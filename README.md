# Runify

<p align="center">
  <strong>轻量级白盒 AI Agent 平台</strong><br>
  低资源占用 · 可视化构建 · 完全可控的自定义 AI Agent
</p>

<p align="center">
  <a href="#-核心设计理念">设计理念</a> •
  <a href="#-轻量特性">轻量特性</a> •
  <a href="#-技术栈">技术栈</a> •
  <a href="#-高级配置指南">配置指南</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-项目结构">项目结构</a> •
  <a href="#-生产部署">生产部署</a> •
  <a href="#-参与贡献">参与贡献</a>
</p>

---

> 💡 **告别黑盒与魔法，一切尽在掌握。**
> 在 Runify 中，你看到的每一步推理、每一次工具调用、每一个决策节点，都是**完全透明、可编辑且可调试**的。我们致力于将算法与工程的确定性，重新完整地交还给开发者。

---

## 轻量运行
Runify 的核心设计目标是**极简化本地资源占用，让开发者无负担地拉起一套 Agent 环境**：

* **低内存占用**：基于 Vert.x 高性能异步框架，系统无需重型依赖。基础运行时内存占用保持在 **100MB~200MB** 之间，启动在 1~2 秒内完成。
* **配置零门槛**：对硬件要求极低，一台普通的个人电脑（Mac/Windows）或入门级云服务器（1核2G）即可流畅跑满本地流式响应。
* **灵活的数据源**：系统底层数据库支持 SQLite 与 PostgreSQL。默认使用极简的 SQLite 本地文件存储，实现零配置极简运行；也可根据生产环境需要，一键切换至 PostgreSQL 协同运行。此外，系统内置 Lucene 检索，无需额外拉起庞大的搜索集群。

## 核心能力

### 自定义 AI Agent

通过直观的拖拽式画布，像搭积木一样自由组合、串联核心节点。摒弃了复杂的 BPMN 规范，采用更符合开发者直觉的 DAG（有向无环图）执行引擎：
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

集成了现代化的富文本编辑器，方便你在打磨 Agent 的同时，随时随地沉淀知识与灵感：
* **多要素渲染**：完美支持标准 Markdown 语法、代码块高亮、数学公式以及图表多媒体混排，提供所见即所得的丝滑编辑体验。
* **无感自动保存**：内置实时全自动保存机制。无论是断网、误触关闭还是设备意外断电，平台都会在后台实时暂存你的每一行输入，彻底告别内容丢失的焦虑。
* **理清设计思路**：它不仅是 Agent 的上下文来源，更是你私人的轻量级知识库。

### 多数据源

平台提供了独立的数据源配置管理，支持一键连接并托管你的外部业务环境，让 Agent 真正具备理解你私有业务数据的能力：
* **SQL 数据库**：完美适配 PostgreSQL、MySQL，支持连接测试与动态驱动管理。
* **缓存供应商**：支持高性能的 Redis 集群或单机版，同时提供零依赖的本地缓存方案。

### 多端适配

为了方便日常运维，前端在视觉呈现上针对不同屏幕尺寸进行了专门的优化：
* **Web 响应式**：采用前端原子化样式（Tailwind CSS）开发，界面布局灵活自适应，在手机端也能流畅处理 Agent 的人机审批。
* **桌面端（Electron）**：支持一键打包为原生桌面应用，为 macOS 与 Windows 用户提供低延迟的单窗口管理环境。


## 项目截图

<!-- 在 docs/images/ 目录下放置项目图片，然后在此处引用 -->
| <img width="400" src="https://github.com/user-attachments/assets/2ea7088b-a1a1-45c4-a235-bc19bcde2b8a" /> | <img width="400" src="https://github.com/user-attachments/assets/565a2868-5c7d-439f-9993-d4a01cb95d9b" /> |
|---|---|
| <img width="400" src="https://github.com/user-attachments/assets/e0f92360-8ff5-483e-99ee-a846b8905714" /> | <img width="400" src="https://github.com/user-attachments/assets/28a3fe5e-dcf3-4e2d-96b2-217420cfb81d" /> |
## 项目视频
<video width="300" src="https://github.com/user-attachments/assets/05e68693-d2f1-41c7-93e9-f8848d243d6b"></video>

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

<img width="300" height="300" alt="c177135ae214abf9370e57496da28f76" src="https://github.com/user-attachments/assets/f6f2f99f-9dce-42f9-b63b-3d9c81d0dd6e" />


## 参与讨论

我们欢迎所有人对 Runify 提出看法、建议和想法，不只是代码贡献。

- **[Discussions](../../discussions)** — 分享你的想法、提出建议、讨论功能需求
- **[Issues](../../issues)** — 提交 Bug 报告、功能请求
- **[Pull Requests](../../pulls)** — 代码贡献

无论你是开发者、设计师、产品经理还是普通用户，你的意见都对我们很重要。

## License

[Apache License 2.0](LICENSE)

所有贡献者需签署 [CLA](CLA.md)。
