import { ref, computed } from 'vue'
import { t } from '@/locales'
import { chatCompletionStream, type ToolCall } from './api'

export type AgentStatus = 'idle' | 'running' | 'paused' | 'stopped' | 'done' | 'error'

export interface LogItem {
  id: number
  kind: 'narration' | 'tool' | 'error' | 'system' | 'user'
  text: string
  toolName?: string
  status?: 'running' | 'success' | 'error'
}

export interface PlanItem {
  text: string
  status: 'pending' | 'doing' | 'done'
}

/** 工具执行的调用元信息（UI 关联用，如把内联画布锚定到该 tool_call） */
export interface ToolCallMeta {
  callId: string
}

/** 循环解析出的可执行工具（宿主上下文已由 runtime 闭包收拢，循环只管 args） */
export interface ResolvedTool {
  execute: (args: Record<string, any>, meta: ToolCallMeta) => Promise<any> | any
  /** 是否改变了宿主内容（画布等），用于批次后整理动作 */
  mutating?: boolean
  /** 变更后跳过整理动作（如坐标已就位无需重排） */
  skipLayout?: boolean
  /** 工具结果回喂模型的截断上限（默认 TOOL_RESULT_LIMIT） */
  resultLimit?: number
}

/**
 * agent 宿主运行时：循环本身与宿主形态（画布/项目）无关，
 * 宿主差异全部通过本接口注入。plan / finish 语义由循环内置：
 * plan 不经 resolveTool；finish 需宿主提供 executor（结果成功即转 done）。
 */
export interface AgentRuntime {
  buildSystemPrompt(): string
  /** 每轮请求前构建本轮下发的工具 schema 列表（可随宿主状态变化） */
  buildTools(): any[]
  resolveTool(name: string): ResolvedTool | undefined
  /** 用户消息装饰（如附带画布快照/项目现状），start 与多轮续聊均会调用 */
  decorateUserMessage?(content: string): string | Promise<string>
  /** 一批工具执行完后的清理动作（无论成败都会调用，如子画布刷新合并） */
  afterToolBatch?(): void
  /** 本轮存在宿主变更时的整理动作（如画布自动布局） */
  onMutatedRound?(): void
}

export interface AgentLoopOptions {
  maxIterations?: number
  /**
   * 自治模式（作为子 agent 被程序驱动，没有用户来点「继续」）：
   * 撞轮次上限自动续跑一次、LLM 层错误自动重试，预算耗尽转 error 终态，
   * 绝不停留在等待人工介入的 paused 态
   */
  autonomous?: boolean
  /**
   * 同一批次的工具并发执行（Promise.all，错误各自隔离；finish 留到批尾单独跑）。
   * 仅当宿主的工具彼此无共享可变状态时开启（如项目 agent 全是 API 调用与子代理调度）；
   * 画布 agent 禁止开启——同一画布上并发改图会乱
   */
  parallelTools?: boolean
}

const DEFAULT_MAX_ITERATIONS = 60
const TOOL_RESULT_LIMIT = 8000
const ARGS_SUMMARY_LIMIT = 80
/** 自治模式预算：撞上限自动续跑次数 / LLM 错误自动重试次数 */
const AUTO_RESUME_LIMIT = 1
const LLM_RETRY_LIMIT = 2

function truncate(text: string, limit: number): string {
  return text.length > limit ? text.slice(0, limit) + '…(truncated)' : text
}

/**
 * 前端驱动的通用 agent 循环（从 useWorkflowAgent 抽取，画布无关）
 * - pause：跑完在途轮（含其全部工具）后不再发起下一轮
 * - stop：中断在途请求，宿主内容保留现状
 * - retry：LLM 层出错时重发同一轮（messages 未被污染）
 */
export function useAgentLoop(runtime: AgentRuntime, options?: AgentLoopOptions) {
  const maxIterations = options?.maxIterations ?? DEFAULT_MAX_ITERATIONS
  const autonomous = options?.autonomous === true
  const parallelTools = options?.parallelTools === true

  const status = ref<AgentStatus>('idle')
  const logs = ref<LogItem[]>([])
  /** 日志内容版本号（含流式追加），供 UI 做浅监听滚动，避免 deep watch 全量遍历 */
  const logsVersion = ref(0)
  const iteration = ref(0)
  const summary = ref('')
  const plan = ref<PlanItem[]>([])

  let messages: any[] = []
  let modelId = ''
  let abortController: AbortController | null = null
  let logSeq = 0
  let narrationLog: LogItem | null = null
  /** 是否有 runLoop 实例在途（防止暂停在途轮后立刻“继续”并发起第二个循环） */
  let loopActive = false
  let autoResumeBudget = 0
  let llmRetryBudget = 0

  const busy = computed(() => status.value === 'running' || status.value === 'paused')

  function pushLog(item: Omit<LogItem, 'id'>): LogItem {
    logs.value.push({ ...item, id: ++logSeq })
    logsVersion.value++
    // 返回 reactive proxy（而非原始对象），后续追加文本才能触发视图更新
    return logs.value[logs.value.length - 1]
  }

  function appendNarration(text: string) {
    if (!text) return
    if (!narrationLog) {
      narrationLog = pushLog({ kind: 'narration', text: '' })
    }
    narrationLog.text += text
    logsVersion.value++
  }

  function toolLogFor(callId: string, name: string): LogItem {
    const existing = logs.value.find((l) => l.kind === 'tool' && (l as any).callId === callId)
    if (existing) return existing
    const log = pushLog({ kind: 'tool', text: '', toolName: name, status: 'running' })
    ;(log as any).callId = callId
    return log
  }

  /** 完成态统一处理（finish 工具与自然结束共用） */
  function markDone(text: string, mutated = false) {
    summary.value = text
    if (mutated) runtime.onMutatedRound?.()
    status.value = 'done'
    pushLog({ kind: 'system', text: t('workflowAgent.log.done') })
  }

  async function executeToolCalls(toolCalls: ToolCall[]): Promise<boolean> {
    try {
      return await doExecuteToolCalls(toolCalls)
    } finally {
      // 宿主批次清理（如子画布刷新按批合并：一批工具里加 30 个子节点也只重渲染一次）
      runtime.afterToolBatch?.()
    }
  }

  async function doExecuteToolCalls(toolCalls: ToolCall[]): Promise<boolean> {
    return parallelTools && toolCalls.length > 1
      ? await executeParallel(toolCalls)
      : await executeSequential(toolCalls)
  }

  /** 执行单个工具调用（含 plan 内置处理与日志），错误收敛为 {error} 结果 */
  async function runToolCall(
    call: ToolCall
  ): Promise<{ result: any; mutated: boolean; args: Record<string, any> }> {
    const name = call.function.name
    const log = toolLogFor(call.id, name)
    let args: Record<string, any> = {}
    let result: any
    let mutated = false
    try {
      args = call.function.arguments ? JSON.parse(call.function.arguments) : {}
      log.text = truncate(JSON.stringify(args), ARGS_SUMMARY_LIMIT)
      if (name === 'plan') {
        // 规划工具：更新面板 todolist，不触发宿主操作
        result = applyPlan(args)
        log.text = truncate(
          plan.value.map((item) => `${item.status === 'done' ? '✓' : '○'} ${item.text}`).join(' / '),
          ARGS_SUMMARY_LIMIT
        )
      } else {
        const tool = runtime.resolveTool(name)
        if (!tool) throw new Error(`未知工具: ${name}`)
        result = await tool.execute(args, { callId: call.id })
        // skipLayout：坐标已就位（如清空画布），不触发整理动作
        if (tool.mutating && !tool.skipLayout) mutated = true
      }
      log.status = 'success'
    } catch (e: any) {
      result = { error: e?.message ?? String(e) }
      log.status = 'error'
      log.text = `${log.text} → ${result.error}`
    }
    return { result, mutated, args }
  }

  function pushToolMessage(call: ToolCall, result: any) {
    messages.push({
      role: 'tool',
      tool_call_id: call.id,
      // get_workflow / get_node_detail 等大结果工具有更高的截断上限（resultLimit）
      content: truncate(
        JSON.stringify(result ?? null),
        runtime.resolveTool(call.function.name)?.resultLimit ?? TOOL_RESULT_LIMIT
      )
    })
  }

  async function executeSequential(toolCalls: ToolCall[]): Promise<boolean> {
    let mutated = false
    for (const call of toolCalls) {
      if (status.value === 'stopped') return mutated
      const outcome = await runToolCall(call)
      if (outcome.mutated) mutated = true
      pushToolMessage(call, outcome.result)
      if (call.function.name === 'finish' && !outcome.result?.error) {
        markDone(outcome.args.summary ?? '', mutated)
        return mutated
      }
    }
    return mutated
  }

  /**
   * 并行批次：普通工具 Promise.all 并发（错误各自隔离），全部落定后按原调用顺序回填
   * tool 消息；finish 宣告完成，留到批尾单独跑
   */
  async function executeParallel(toolCalls: ToolCall[]): Promise<boolean> {
    let mutated = false
    const normalCalls = toolCalls.filter((call) => call.function.name !== 'finish')
    const finishCalls = toolCalls.filter((call) => call.function.name === 'finish')
    const outcomes = new Map<string, Awaited<ReturnType<typeof runToolCall>>>()
    await Promise.all(
      normalCalls.map(async (call) => {
        outcomes.set(call.id, await runToolCall(call))
      })
    )
    for (const call of normalCalls) {
      const outcome = outcomes.get(call.id)!
      if (outcome.mutated) mutated = true
      pushToolMessage(call, outcome.result)
    }
    for (const call of finishCalls) {
      if (status.value === 'stopped') return mutated
      const outcome = await runToolCall(call)
      pushToolMessage(call, outcome.result)
      if (!outcome.result?.error) {
        markDone(outcome.args.summary ?? '', mutated)
        return mutated
      }
    }
    return mutated
  }

  function applyPlan(args: Record<string, any>): any {
    const items = Array.isArray(args.items) ? args.items : []
    const valid = items.filter(
      (item: any) => item && typeof item.text === 'string' && item.text.trim()
    )
    if (!valid.length) throw new Error('items 不能为空')
    plan.value = valid.map((item: any) => ({
      text: item.text.trim(),
      status: ['pending', 'doing', 'done'].includes(item.status) ? item.status : 'pending'
    }))
    return { ok: true }
  }

  async function runLoop() {
    // 同一时刻只允许一个循环实例：流式在途时被暂停又立刻“继续”，
    // 原循环还活着，翻回 running 让它继续即可，绝不能再起第二个循环
    if (loopActive) return
    loopActive = true
    try {
      await doRunLoop()
    } finally {
      loopActive = false
    }
  }

  async function doRunLoop() {
    while (status.value === 'running') {
      if (++iteration.value > maxIterations) {
        iteration.value = maxIterations
        // 自治模式没有用户来点「继续」：预算内自动续跑，耗尽即失败终态
        if (autonomous) {
          if (autoResumeBudget > 0) {
            autoResumeBudget--
            iteration.value = 0
            pushLog({ kind: 'system', text: t('workflowAgent.log.autoResumed') })
            continue
          }
          status.value = 'error'
          pushLog({ kind: 'error', text: t('workflowAgent.log.budgetExhausted') })
          return
        }
        // 达到轮次上限不算失败：自动暂停，用户点「继续」后重置计数接着跑
        status.value = 'paused'
        pushLog({ kind: 'system', text: t('workflowAgent.log.maxIterations') })
        return
      }
      abortController = new AbortController()
      narrationLog = null
      let resp
      try {
        // 每轮由宿主按自身状态决定传哪些工具（如 clear_workflow 仅画布非空时给出）
        resp = await chatCompletionStream(
          modelId,
          { messages, tools: runtime.buildTools() },
          abortController.signal,
          {
            onDelta: appendNarration,
            onToolCallStart: ({ id, name }) => toolLogFor(id, name)
          }
        )
      } catch (e: any) {
        if ((status.value as AgentStatus) === 'stopped') return
        // LLM 层错误：messages 未污染，可重试本轮
        iteration.value--
        if (autonomous && llmRetryBudget > 0) {
          llmRetryBudget--
          pushLog({ kind: 'system', text: t('workflowAgent.log.retrying') })
          continue
        }
        status.value = 'error'
        pushLog({ kind: 'error', text: e?.message ?? String(e) })
        return
      }
      const toolCalls = resp.toolCalls ?? []
      messages.push({
        role: 'assistant',
        content: resp.content || '',
        // thinking 模型要求思考内容按原字段名回传（如 reasoning_content），否则下一轮 400
        ...(resp.reasoningKey && resp.reasoningContent
          ? { [resp.reasoningKey]: resp.reasoningContent }
          : {}),
        ...(toolCalls.length ? { tool_calls: toolCalls } : {})
      })
      if (!toolCalls.length) {
        markDone(resp.content || '')
        return
      }
      const mutated = await executeToolCalls(toolCalls)
      if (mutated && busy.value) runtime.onMutatedRound?.()
      if ((status.value as AgentStatus) === 'paused') {
        pushLog({ kind: 'system', text: t('workflowAgent.log.paused') })
        return
      }
    }
  }

  /**
   * 用户消息装饰：修改场景不能依赖模型自觉查询现状，
   * 每次用户发话都由宿主强制附带最新现状（画布快照/处理器清单）；装饰失败回落原文
   */
  async function decorate(content: string): Promise<string> {
    if (!runtime.decorateUserMessage) return content
    try {
      return await runtime.decorateUserMessage(content)
    } catch {
      return content
    }
  }

  async function start(requirement: string, model: string) {
    // 宿主内容保持原样：增量修改还是清空重建由 AI 自行决策
    modelId = model
    logs.value = []
    summary.value = ''
    plan.value = []
    iteration.value = 0
    logSeq = 0
    autoResumeBudget = AUTO_RESUME_LIMIT
    llmRetryBudget = LLM_RETRY_LIMIT
    status.value = 'running'
    pushLog({ kind: 'system', text: t('workflowAgent.log.started') })
    // 首条需求进对话流（对话形态下起手那句必须可见）；装饰后的现状附注只进模型上下文
    pushLog({ kind: 'user', text: requirement })
    messages = [
      { role: 'system', content: runtime.buildSystemPrompt() },
      { role: 'user', content: await decorate(requirement) }
    ]
    runLoop()
  }

  function pause() {
    if (status.value === 'running') {
      status.value = 'paused'
    }
  }

  function resume() {
    if (status.value === 'paused') {
      // 继续时重置轮次预算（含达到上限后的续跑）
      iteration.value = 0
      status.value = 'running'
      pushLog({ kind: 'system', text: t('workflowAgent.log.resumed') })
      runLoop()
    }
  }

  function stop() {
    if (status.value === 'running' || status.value === 'paused' || status.value === 'error') {
      status.value = 'stopped'
      abortController?.abort()
      pushLog({ kind: 'system', text: t('workflowAgent.log.stopped') })
    }
  }

  /**
   * 继续沟通：在同一会话上下文里追加用户消息并续跑
   * done（生成完成后提修改意见）与 paused（暂停中插话）时可用；
   * stopped 不可用——中断可能发生在工具批次中间，messages 里存在未应答的 tool_calls
   */
  async function send(message: string) {
    if (status.value !== 'done' && status.value !== 'paused') return
    const content = message.trim()
    if (!content) return
    // 日志只展示用户原话，装饰后的现状附注仅进对话上下文
    pushLog({ kind: 'user', text: content })
    iteration.value = 0
    autoResumeBudget = AUTO_RESUME_LIMIT
    llmRetryBudget = LLM_RETRY_LIMIT
    status.value = 'running'
    messages.push({ role: 'user', content: await decorate(content) })
    runLoop()
  }

  /** LLM 层错误后重试：直接重发同一轮 */
  function retry() {
    if (status.value === 'error') {
      status.value = 'running'
      pushLog({ kind: 'system', text: t('workflowAgent.log.retrying') })
      runLoop()
    }
  }

  function reset() {
    status.value = 'idle'
    logs.value = []
    summary.value = ''
    plan.value = []
    iteration.value = 0
  }

  return { status, logs, logsVersion, plan, iteration, summary, busy, start, send, pause, resume, stop, retry, reset }
}

export type AgentLoop = ReturnType<typeof useAgentLoop>
