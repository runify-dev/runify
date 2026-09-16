/**
 * ai-generate 模块的全部类型定义（单一来源）。
 * 纯类型、零运行时依赖：值/函数在 common.ts（公共函数）、tools.ts（工具构建）、create-agent.ts、index.ts。
 * 面板（生成对话框）自己的展示类型在 panel/type.ts。
 */

// ============================================================
// SSE / 聊天补全（原 api.ts）
// ============================================================

export interface ToolCall {
  id: string
  type: string
  function: { name: string; arguments: string }
}

export interface ChatCompletionResult {
  content: string
  toolCalls: ToolCall[]
  finishReason: string | null
  usage?: Record<string, any> | null
  /** thinking 模型的思考内容及其原始字段名（回传 assistant 消息时按原字段名带回） */
  reasoningKey?: string | null
  reasoningContent?: string | null
}

/**
 * 通用流式订阅者（对应后端 reactive 流的接口约定）：
 *   void onNext(T item);
 *   default void onComplete(Optional<Throwable> error) {}
 * onNext 逐条收流式条目；onComplete 收尾（error 有值即失败，无值即正常结束）。
 * SSE 层（chatCompletion）与 agent 层（create_agent 的对外输出 CALL）复用同一接口。
 */
export interface Call<T> {
  onNext(item: T): void
  onComplete?(error?: Error): void
}

/**
 * 后端一轮流式补全逐条下发的条目：
 * - delta / reasoning：增量文本
 * - tool_call：某工具调用的一个片段（id + name + 本片 arguments 增量，非累计）。
 *   前端按 id group：首次见到即建卡（name 已带），后续拼接 arguments 实时展示。
 * - completion：整轮最终结果（含累积后的完整 toolCalls，权威值，用于执行/回填下一轮）
 */
export type StreamItem =
  | { type: 'delta'; content: string }
  | { type: 'reasoning'; content: string }
  | { type: 'tool_call'; id: string; name: string; arguments: string }
  | { type: 'completion'; result: ChatCompletionResult }

export interface SseEvent {
  type: 'delta' | 'reasoning' | 'tool_call' | 'completion' | 'error'
  [key: string]: any
}

/**
 * 兼容旧调用方（useAgentLoop）：把 Call 订阅式包成「分散回调 + Promise 返回本轮 completion」。
 * ⚠️ 过渡期垫片，随 useAgentLoop 一起迁到 create_agent 后删除。
 */
export interface ChatCompletionCallbacks {
  onDelta?: (text: string) => void
  onReasoning?: (text: string) => void
  onToolCallStart?: (info: { id: string; name: string }) => void
}

// ============================================================
// Agent 事件 / 配置 / 绑定（原 agent-config.ts）
// ============================================================

/**
 * create_agent 逐条对外下发的事件负载：
 * - narration / reasoning：assistant 正文 / 思考的增量文本
 * - tool_start：某工具首次出现（建卡，name 已带）
 * - tool_args：该工具入参 JSON 的增量（流式拼接展示）
 * - tool_end：该工具执行完（ok + 结果/错误摘要）
 * - done / error：本 agent 终态（随后 Call.onComplete 收尾）。done 带 data＝本 agent 交付
 *   （顶层父 agent 隐式结束时即给用户看的总结文本），与 AgentResult.data 同源。
 */
export type AgentPayload =
  | { type: 'narration'; delta: string }
  | { type: 'reasoning'; delta: string }
  | { type: 'tool_start'; id: string; name: string }
  | { type: 'tool_args'; id: string; delta: string }
  | { type: 'tool_end'; id: string; ok: boolean; result: string }
  | { type: 'done'; data?: any }
  | { type: 'error'; message: string }

/**
 * 一条对外事件 = 负载 + position（层级路径）。
 * position 是从整棵事件树根到「本事件所属 agent」的键链：
 *   - 顶层 agent 的事件：position = []
 *   - 某委派工具起的子 agent，其所有事件：position = [...父position, 语义键]（如被配置节点的 nodeId）
 * 语义键由委派工具组合（见 useAgentConfig 用法），故同一父 agent 内多次委派互不相撞；
 * 消费方仅凭 position 即可判断分组与嵌套如何展示，无需预先拼子树——整条流扁平、带坐标。
 */
export type AgentEvent = AgentPayload & { position: string[] }

/**
 * 一次工具执行的「执行器」（CompletableFuture 风格）：既能 await 拿结果，又能 cancel 取消。
 * 委派型工具（起子 agent 这类长活）返回它；createAgent 调用工具那层会把 cancel 登记进本 agent
 * 的停止表，父 stop() 时逐个 cancel → 级联停子。普通工具返回值/Promise 即可，无需构造它。
 */
export interface Executor {
  /** 结果 promise（createAgent await 它拿回喂模型的结果） */
  promise: Promise<any>
  /** 取消：中断这次执行（委派工具里就是子 agent 的 stop） */
  cancel: () => void
}

/** 模型可调用的一个函数（全局结构工具与节点工具同构，就这 4 个字段） */
export interface AgentTool {
  /** 函数名，全局唯一（下发给 LLM 的 tool 名） */
  name: string
  /** 函数说明（下发给 LLM） */
  description: string
  /** 参数 JSON Schema（下发给 LLM） */
  parameters: Record<string, any>
  /**
   * 执行体。框架零注入——只吃模型给的 args；lf / modelId / call / position 及模块自定义数据一律在
   * 构造期（useAgentConfig 把 bindings 递给 tools 构建器时）闭包进来；node 用 resolveNode(lf, id) 自取。
   * 需可取消的委派型工具返回 Executor（见其说明），普通工具返回值/Promise 即可。
   * @param args 模型传入参数（已 JSON.parse）
   */
  apply: (args: Record<string, any>) => any | Promise<any> | Executor
  /**
   * 该工具结果回喂模型时的截断上限（字符）。默认走全局 TOOL_RESULT_LIMIT；
   * 静态、每次只拉一次的大目录（如 list_nodes 全量签名）可设 Infinity 不截断，避免模型看到 …(truncated) 反复重调。
   */
  resultLimit?: number
}

/**
 * 构造期绑定：既是 useAgentConfig 的入参，也是 tools 构建器拿到的东西。纯数据，无 ctx 包装。
 */
export interface AgentBindings {
  /** 本作用域 LogicFlow 实例（顶层=主 lf，子 agent=子画布 lf）；改图 / resolveNode 用 */
  lf: any
  /** 模型资源 id（委派起子 agent 复用同一模型） */
  modelId: string
  /** 对外事件出口；createAgent 发自身事件、委派工具递归子 agent 都用它 */
  call: Call<AgentEvent>
  /** 画布类型（APPLICATION 等，取 WorkflowType 枚举值）；按画布选节点变体、并随委派透传给子 agent */
  workflowType: string
  /** 事件树层级路径（顶层=[]）；委派工具起子 agent 时在其后追加语义键 */
  position?: string[]
  /** 循环轮次上限，缺省 60 */
  maxIterations?: number
  /**
   * 根画布 lf（主画布）。跨层「全景」查询（get_canvas_detail）用它从根往下递归所有循环体子画布，
   * 也是按 lf_id 用 getSubLf 展开循环体子画布的入口；经委派链透传，顶层主画布缺省即自身 lf。
   */
  rootLf?: any
  /**
   * 当前所在循环层上下文（主画布上为空；configure_node 据 lf_id 推出并注入循环体内节点的子 agent）：
   * - depth：层深（最外层循环体=0，逐层 +1）
   * - selfLoopId：当前所在这层循环的真实节点 id（引用「当前项」= [selfLoopId,"item"]）
   * - outerLoopId：外层循环的真实 id（无外层则空）
   * 供 get_canvas_detail 标注 youAreHere、供工具节点选对引用层。
   */
  loopContext?: { depth: number; selfLoopId: string; outerLoopId?: string }
  /**
   * 本次生成的整体目标背景（顶层 = 用户需求）。useAgentConfig 把它折进本 agent 的 system 提示，
   * 委派工具（configure_node / execute_step）透传给子 agent，使各层配置时都"知情"整体链路，而不改变各自只做本职的职责。
   */
  context?: string
  /**
   * 本 agent 的 messages 历史（活引用，跨轮累积）。作者不填、由框架经手：
   * - 委派工具起子 agent 时，传入一段「初始历史」（父从自己这条裁出来的一份，见 tools.ts 的 inheritMessages），
   *   子由此接着父的对话往下配，不必从画布重猜自己为何被建、该配成什么样；不传即从空历史起。
   * - useAgentConfig 会把它规整成 [system 头, ...初始历史] 并回写这里，同时交给 createAgent 直接在其上追加每轮往来。
   * 于是它同时是「本 agent 的活历史」与「起下一级子时的裁剪来源」——一份数据，无需区分新旧或出处。
   */
  messages?: any[]
}

/** 一台 agent 的声明式配置 —— 每个节点/画布向引擎贡献的单元（作者写这个） */
export interface AgentConfig {
  /**
   * 节点级：本节点是什么、能干什么。
   * 汇总进父 agent 的「可用节点类型」选型清单；父 agent 写循环体委派语时也读它。
   * 聚合成引擎（顶层）时不填。
   */
  description?: string
  /** 详细用法说明（字段怎么填、输出怎么引用、约束），拼进系统提示词 */
  prompt: string
  /**
   * 工具列表（已闭包好各自需要的 lf / call / node 等）。
   * 唯一的 `bindings => tools` 边界落在各画布的装配工厂（useXxxAgent）上：它拿到构造期 bindings
   * 后就地把工具建好，塞进这个字段——故 config 本身是纯数据、无 lambda 字段，engine 直接透传。
   */
  tools: AgentTool[]
}

/**
 * 节点模块的默认导出（每个节点 agent.ts）：type + 输出字段 + useAgent 工厂。
 * useAgent(bindings) 产出本节点的 AgentConfig；configure_node（或变体分派）再用 useAgentConfig 装配。
 */
export interface NodeAgent {
  /** 节点类型（= node.type） */
  type: string
  /** 按 bindings 产出本节点的 AgentConfig */
  useAgent: (bindings: AgentBindings) => AgentConfig
}

/**
 * 装配好、可交给 createAgent 跑的 agent：纯合并 = 声明式 config + 构造期 bindings。
 * 运行期数据（modelId/call/position）就在自身；跨轮 messages 与中止机件由 createAgent 持有。
 */
export interface RunnableAgent {
  prompt: string
  /**
   * messages 历史（活引用）：system 头 + 传入的初始历史。useAgentConfig 建好、并回写 bindings.messages；
   * createAgent 直接在其上追加每轮往来。
   */
  messages: any[]
  /** 工具列表（装配工厂已闭包好 bindings 建好） */
  tools: AgentTool[]
  modelId: string
  call: Call<AgentEvent>
  position: string[]
  maxIterations: number
}

// ============================================================
// createAgent 句柄 / 结果（原 create-agent.ts）
// ============================================================

/** 一次 run 的终态结果（供父 agent await 子 agent 时读取） */
export interface AgentResult {
  status: 'done' | 'error' | 'stopped'
  /**
   * 本 agent 的交付内容（原 summary 升级为通道）：
   * - 隐式结束（模型不再发工具）＝最后一段正文文本（string）；
   * - 经终止工具（返回 { status:'stop', data }）结束＝该工具交出的结构化 data。
   * 委派工具（configure_node/plan/validate）把它当子 agent 的成品上抛给父。
   */
  data?: any
}

/**
 * 工具「交卷并终止」信号：某工具想结束它所在的 agent、把 data 作为该 agent 的交付上抛给父时，
 * 返回它（而非普通结果）。createAgent 的循环一旦发现某工具返回 status==='stop'，即短路收尾、不再
 * 进下一轮，并以 { status:'done', data } 作为该 agent 的 AgentResult（stop 只活在工具返回值这一层，
 * 不冒泡进 AgentResult，故委派工具透传子结果时不会误停父）。
 * 普通工具无需关心它——返回裸结果即"继续"（success 语义）；抛错/返回 { error } 即失败，回喂模型自愈。
 */
export interface AgentStop<T = any> {
  status: 'stop'
  data?: T
}

/** createAgent 的返回句柄 */
export interface AgentHandle {
  /** 跑一轮：追加一条 user 消息、复用历史循环到终态。非重入，等上一次 settle 再调 */
  run: (requirement: string) => Promise<AgentResult>
  /** 停止：中断在途请求 + 逐个跑 onStop 回调（级联到委派中的子 agent） */
  stop: () => void
  /** 注册停止回调（委派工具用它挂子 agent 的 cancel，实现级联停止） */
  onStop: (fn: () => void) => void
}

// ============================================================
// 节点函数签名（原 node-signature.ts）
// ============================================================

/**
 * 字段的类型结构（可递归）：复合值（array 元素、object 字段）把子结构逐个类型化，不塞进散文 description。
 * - type='array'：元素为对象时，用 items 列出元素的字段；元素为标量时省略 items。
 * - type='object'：用 fields 列出对象的字段。
 */
export interface FieldShape {
  /** 字段键（在其所属对象内的键名） */
  value: string
  /** 值类型 */
  type: 'string' | 'number' | 'boolean' | 'array' | 'object' | 'any'
  /** 显示名（可选，顶层出参必填） */
  label?: string
  /** 补充说明（如取值含义、JSON 字符串等），非结构本身 */
  description?: string
  /** 枚举取值（如 finishReason 的 "stop" | "tool_calls"）：值域固定时列出，供下游 judge 精确分流 */
  enum?: readonly string[]
  /** type='array' 且元素为对象时：元素的字段结构 */
  items?: FieldShape[]
  /** type='object' 时：对象的字段结构 */
  fields?: FieldShape[]
}

export interface OutputDef extends FieldShape {
  /** 引用键：下游用 [节点ID, value] 引用 */
  value: string
  /** 显示名（field_list 的 label，顶层必填）。凡能被引用的出参都在 field_list 里、画布上可见 */
  label: string
}

// ============================================================
// 节点注册表签名（原 node-agents.ts）
// ============================================================

/** 每个节点模块导出的入口签名（与画布 useAgent 同构） */
export type NodeUseAgent = (bindings: AgentBindings) => RunnableAgent

/** 入参签名：即 update 工具的 JSON Schema parameters（单一来源，节点内 update.parameters 复用它） */
export interface NodeInputSchema {
  type: string
  properties: Record<string, any>
  required?: string[]
}

/**
 * 每个节点 agent.ts 的统一「函数描述符」（default 导出）：把节点当函数暴露给组合层。
 * - type：节点类型（= list_nodes 的值）
 * - useAgent：配置该节点的子 agent（填 knob 的专家）
 * - input：入参签名（update 的 parameters）
 * - outputs：出参签名（带类型）；出参随配置变的节点（extract/loop）用 (nodeData)=>OutputDef[]
 */
export interface NodeAgentModule {
  type: string
  useAgent: NodeUseAgent
  input: NodeInputSchema
  outputs: OutputDef[] | ((nodeData: any) => OutputDef[])
}
