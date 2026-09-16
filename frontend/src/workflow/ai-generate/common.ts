import { postStream } from '@/request/admin/index'
import { NODE_MENU_GROUPS } from '@/workflow/common/node-group'
import type {
  AgentConfig,
  AgentBindings,
  AgentTool,
  RunnableAgent,
  Call,
  StreamItem,
  SseEvent,
  ChatCompletionResult,
  ChatCompletionCallbacks,
  OutputDef
} from './type'

/**
 * ai-generate 的公共工具函数：跨模块共用的纯函数与基础设施——
 * 节点取值（resolveNode）、agent 装配（useAgentConfig/toToolSchemas）、SSE 补全（chatCompletion）、
 * 节点目录/模板（listNodeCatalog/findNodeTemplate）、节点注册表（nodeUseAgentFor 等）、
 * 出参派生（deriveFieldList）、调试日志（logDebug）。
 * 具体的 Agent 工具（AgentTool 构建器）在 tools.ts，主流程在 index.ts。
 */

// ============================================================
// agent 装配 / 节点取值（原 agent-config.ts）
// ============================================================

const DEFAULT_MAX_ITERATIONS = 60

/** 从 lf 按 id 取节点 model（LogicFlow 标准 API，带 graphModel 兜底）；工具需要 node 时自取 */
export function resolveNode(lf: any, nodeId: string): any | null {
  if (!lf) return null
  const byApi = lf.getNodeModelById?.(nodeId)
  if (byApi) return byApi
  return lf.graphModel?.nodes?.find((n: any) => n.id === nodeId) ?? null
}

/**
 * 装配可运行 agent：把已建好工具的 config 与构造期 bindings 纯合并产出 RunnableAgent。
 * 工具已在各画布装配工厂（useXxxAgent）里就地闭包 bindings 建好，故本函数只把 config 的
 * prompt / tools 透传，并归一化运行期数据（position 缺省 []）。委派型工具自行
 * createAgent(useXxxAgent({...})).run() 递归起子 agent，把子 stop 经 Executor 交 createAgent
 * 挂级联。中止机件全在 createAgent，本函数不碰。
 */
export function useAgentConfig(config: AgentConfig, bindings: AgentBindings): RunnableAgent {
  // 本 agent 自己这条 system：节点/画布 prompt + 折进整体目标背景（自顶层经 bindings.context 逐层透传，供理解链路）。
  const ownPrompt = bindings.context
    ? `${config.prompt}\n\n【本次生成的整体目标（背景，供理解链路；你仍只做自己那份）】\n${bindings.context}`
    : config.prompt
  // system 由本函数统一负责（唯一负责人，保证恰好一条 system 且在首位，否则多条/非首位 system 会 400）：
  // 把继承来的父 system 内容【并入】本 agent 的 system（而非丢弃）——于是子 agent 拿回父的建图范式/手册，
  // 不必从零发挥（父手册在前作背景、子自己那条在后作本职）。非 system 历史原样接在后面。
  const src = bindings.messages ?? []
  const inheritedSystem = src
    .filter((m: any) => m?.role === 'system')
    .map((m: any) => m?.content)
    .filter(Boolean)
    .join('\n\n')
  const nonSystem = src.filter((m: any) => m?.role !== 'system')
  const prompt = inheritedSystem ? `${inheritedSystem}\n\n──────\n\n${ownPrompt}` : ownPrompt
  // 结果回写 bindings.messages（活引用）：本 agent 的委派工具就地读它、裁一份作为下一级子的初始历史；
  // createAgent 也在这同一条上追加往来。
  const messages: any[] = [{ role: 'system', content: prompt }, ...nonSystem]
  bindings.messages = messages
  return {
    prompt,
    messages,
    tools: config.tools,
    modelId: bindings.modelId,
    call: bindings.call,
    position: bindings.position ?? [],
    maxIterations: bindings.maxIterations ?? DEFAULT_MAX_ITERATIONS
  }
}

/** 把 tools 转成 OpenAI function 下发 schema */
export function toToolSchemas(tools: AgentTool[]): any[] {
  return tools.map((tool) => ({
    type: 'function',
    function: {
      name: tool.name,
      description: tool.description,
      parameters: tool.parameters
    }
  }))
}

// ============================================================
// SSE 聊天补全代理（原 api.ts）
// ============================================================

/**
 * 增量解析 SSE 文本块，返回完整事件列表与剩余未完整的 buffer
 * 纯函数，便于单测
 */
export function parseSseChunk(buffer: string): { events: (SseEvent | 'DONE')[]; rest: string } {
  const events: (SseEvent | 'DONE')[] = []
  const parts = buffer.split('\n\n')
  const rest = parts.pop() ?? ''
  for (const part of parts) {
    for (const line of part.split('\n')) {
      if (!line.startsWith('data: ')) continue
      const data = line.slice(6).trim()
      if (data === '[DONE]') {
        events.push('DONE')
        continue
      }
      try {
        events.push(JSON.parse(data) as SseEvent)
      } catch {
        // 忽略无法解析的行
      }
    }
  }
  return { events, rest }
}

/**
 * 调用后端聊天补全代理（SSE），把每条流式条目喂给 call.onNext，收尾走 call.onComplete：
 * - completion 事件作为一条 StreamItem 下发后 onComplete()（正常结束）；
 * - error 事件 / 非 SSE 响应 / 网络错误 / 未收到 completion 一律走 onComplete(error)。
 * 这是 create_agent 内部打后端的唯一入口（不再暴露分散回调 + Promise 返回值）。
 */
export async function chatCompletion(
  modelId: string,
  payload: { messages: any[]; tools?: any[] },
  call: Call<StreamItem>,
  signal: AbortSignal
): Promise<void> {
  try {
    const response: Response = await postStream(
      `/admin/api/model/resources/${modelId}/chat-completion`,
      payload,
      undefined,
      signal
    )

    const contentType = response.headers.get('Content-Type') ?? ''
    if (!contentType.includes('text/event-stream')) {
      // 后端参数/模型错误走普通 JSON（Result 结构）
      const result = await response.json().catch(() => null)
      throw new Error(result?.message ?? `HTTP ${response.status}`)
    }
    if (!response.body) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let completed = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const { events, rest } = parseSseChunk(buffer)
      buffer = rest
      for (const event of events) {
        if (event === 'DONE') continue
        switch (event.type) {
          case 'delta':
            call.onNext({ type: 'delta', content: event.content ?? '' })
            break
          case 'reasoning':
            call.onNext({ type: 'reasoning', content: event.content ?? '' })
            break
          case 'tool_call':
            call.onNext({ type: 'tool_call', id: event.id, name: event.name, arguments: event.arguments ?? '' })
            break
          case 'completion':
            completed = true
            call.onNext({
              type: 'completion',
              result: {
                content: event.content ?? '',
                toolCalls: event.toolCalls ?? [],
                finishReason: event.finishReason ?? null,
                usage: event.usage ?? null,
                reasoningKey: event.reasoningKey ?? null,
                reasoningContent: event.reasoningContent ?? null
              }
            })
            break
          case 'error':
            throw new Error(event.message ?? 'LLM stream error')
        }
      }
    }

    if (!completed) throw new Error('Stream ended without completion event')
    call.onComplete?.()
  } catch (error: any) {
    call.onComplete?.(error instanceof Error ? error : new Error(String(error)))
  }
}

/**
 * 兼容旧调用方（useAgentLoop）：把 Call 订阅式包成「分散回调 + Promise 返回本轮 completion」。
 * ⚠️ 过渡期垫片，随 useAgentLoop 一起迁到 create_agent 后删除。
 */
export async function chatCompletionStream(
  modelId: string,
  payload: { messages: any[]; tools?: any[] },
  signal: AbortSignal,
  callbacks: ChatCompletionCallbacks = {}
): Promise<ChatCompletionResult> {
  let result: ChatCompletionResult | null = null
  let streamError: Error | undefined
  await chatCompletion(
    modelId,
    payload,
    {
      onNext: (item) => {
        switch (item.type) {
          case 'delta':
            callbacks.onDelta?.(item.content)
            break
          case 'reasoning':
            callbacks.onReasoning?.(item.content)
            break
          case 'tool_call':
            // 旧循环只需「开始调用某工具」的信号建卡；按 id group 幂等（toolLogFor 见同 id 返回同卡）
            callbacks.onToolCallStart?.({ id: item.id, name: item.name })
            break
          case 'completion':
            result = item.result
            break
        }
      },
      onComplete: (error) => {
        streamError = error
      }
    },
    signal
  )
  if (streamError) throw streamError
  if (!result) throw new Error('Stream ended without completion event')
  return result
}

// ============================================================
// 出参派生 / 工具节点公共出参（原 node-signature.ts）
// ============================================================

/**
 * 工具类节点（terminal/read-file/apply-patch/glob/grep/list-dir/create-file/file-upload/file-download/
 * list-skills/download-skills/run-skill）共有的「工具执行」输出：完整工具调用结果。
 * agent 场景由 context-push 用 [节点ID,"tool"] 引用它回写对话上下文。
 */
export const TOOL_OUTPUT: OutputDef = {
  value: 'tool',
  label: '工具执行',
  type: 'object',
  description: 'agent 场景用 context-push 引用它回写对话上下文',
  fields: [
    { value: 'toolName', type: 'string', label: '工具名' },
    { value: 'content', type: 'string', label: '执行结果文本' },
    { value: 'functionArguments', type: 'string', label: '调用参数', description: 'JSON 字符串' },
    { value: 'status', type: 'string', label: '执行状态' }
  ]
}

/** 由 outputs 派生 field_list —— 出参的单一来源，避免与 field_list 两处重复/漂移 */
export function deriveFieldList(outputs: OutputDef[]): { label: string; value: string }[] {
  return outputs.map((o) => ({ label: o.label, value: o.value }))
}

// ============================================================
// 节点目录 / 模板（原 node-catalog.ts）
// ============================================================

/**
 * 当前工作流类型的「可用节点目录」（按分组，含 type 与名称）。
 * 供 list_nodes 工具按需返回给模型——模型规划要新增节点时才拉取，不注入系统提示词
 * （多轮续聊里只改配置/连线时无需目录，做成工具可省掉这份常驻上下文）。
 * 数据源与画布节点菜单同一份（node-group 的 NODE_MENU_GROUPS），保证模型可选节点与用户能拖的一致。
 * 只给简易版（type + 名称）：够选型；单节点字段/配置细节由 configure_node 委派子 agent 负责。
 */
export function listNodeCatalog(
  workflowType: string
): Array<{ group: string; nodes: Array<{ type: string; name: string }> }> {
  const groups = NODE_MENU_GROUPS[workflowType] ?? []
  return groups.map((g) => ({
    group: g.label,
    nodes: g.nodes.map((n: any) => ({ type: n.type, name: n.properties?.name ?? '' }))
  }))
}

/**
 * 预建索引：workflowType → (type → 节点模板对象)。NODE_MENU_GROUPS 静态，模块加载时构建一次，
 * 之后 findNodeTemplate 直接 O(1) 查表，避免每次 add_node 都遍历分组。
 */
const NODE_TEMPLATE_INDEX: Record<string, Record<string, any>> = (() => {
  const index: Record<string, Record<string, any>> = {}
  for (const workflowType in NODE_MENU_GROUPS) {
    const byType: Record<string, any> = {}
    for (const g of NODE_MENU_GROUPS[workflowType]) {
      for (const n of g.nodes as any[]) byType[n.type] = n
    }
    index[workflowType] = byType
  }
  return index
})()

/**
 * 按 workflowType + type 查节点模板对象（含 properties：name/width/field_list/nodeData 默认值）。
 * add_node 用它：查不到＝当前画布不支持该类型（校验）；查到则照菜单点选路径（node.ts 的 appendNode）
 * 用模板的 properties 建节点，保证 AI 建的节点与手动拖的默认结构一致。
 * 注意：start-node 是画布固定入口、不由 add_node 新增，本就不在菜单集合里。
 */
export function findNodeTemplate(workflowType: string, type: string): any | undefined {
  return NODE_TEMPLATE_INDEX[workflowType]?.[type]
}

// 注意：节点配置 agent 注册表（import.meta.glob 急切加载所有节点 agent.ts）在 tools.ts，不在这里——
// 它若放本文件会与「节点 agent.ts 在顶层引用 common 的 TOOL_OUTPUT/deriveFieldList」成循环依赖（TDZ）。
// common.ts 保持无 glob，才能被节点安全引用。见 tools.ts 的「节点配置 agent 注册表」段。

// ============================================================
// 生成过程调试日志（原 debug-log.ts）
// ============================================================

/**
 * 生成过程调试日志（写 localStorage，供人工复制出来排查）。
 * 关键在于每条都带 position（层级路径）——能看出"哪一层的哪个 agent 建/配了什么"，
 * layer 搭错、漏节点、引用错这类问题一眼可见。仅调试用，任何异常都吞掉、绝不影响生成。
 *
 * 复制方法（浏览器控制台）：copy(localStorage.getItem('ai-generate-debug-log'))
 */
const DEBUG_KEY = 'ai-generate-debug-log'
const DEBUG_MAX_ENTRIES = 4000
const DEBUG_MAX_CHARS = 3_000_000 // ~3MB，超了从头丢老的

/** 安全序列化并按上限截断（单字段别把整份 blueprint/长 prompt 塞爆） */
export function clipForLog(v: any, limit = 6000): any {
  if (v === undefined) return undefined
  let s: string
  try {
    s = typeof v === 'string' ? v : JSON.stringify(v)
  } catch {
    s = String(v)
  }
  if (s.length <= limit) return v
  return (typeof v === 'string' ? s : s).slice(0, limit) + `…(+${s.length - limit}字)`
}

/** 追加一条日志。entry 里 pos=position 数组、kind=类别、其余自定义 */
export function logDebug(entry: Record<string, any>): void {
  try {
    if (typeof localStorage === 'undefined') return
    const raw = localStorage.getItem(DEBUG_KEY)
    const arr: any[] = raw ? JSON.parse(raw) : []
    arr.push({ ts: new Date().toISOString().slice(11, 23), ...entry })
    while (arr.length > DEBUG_MAX_ENTRIES) arr.shift()
    let out = JSON.stringify(arr)
    while (out.length > DEBUG_MAX_CHARS && arr.length > 1) {
      arr.shift()
      out = JSON.stringify(arr)
    }
    localStorage.setItem(DEBUG_KEY, out)
  } catch {
    /* localStorage 不可用/超限：忽略，不影响生成 */
  }
}

/** 清空日志（每次新生成开始时调，避免与上一次混在一起） */
export function clearDebugLog(): void {
  try {
    if (typeof localStorage !== 'undefined') localStorage.removeItem(DEBUG_KEY)
  } catch {
    /* ignore */
  }
}
