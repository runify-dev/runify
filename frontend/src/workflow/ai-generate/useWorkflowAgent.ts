import { ref, computed } from 'vue'
import { t } from '@/locales'
import { chatCompletionStream, type ToolCall } from './api'
import {
  toolExecutors,
  buildActiveToolSchemas,
  flushLoopRefresh,
  buildWorkflowSnapshot,
  hasCanvasContent,
  type WorkflowAgentContext
} from './tools'
import { buildSystemPrompt } from './prompt'

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

const MAX_ITERATIONS = 60
const TOOL_RESULT_LIMIT = 8000
const ARGS_SUMMARY_LIMIT = 80

function truncate(text: string, limit: number): string {
  return text.length > limit ? text.slice(0, limit) + '…(truncated)' : text
}

/**
 * 前端驱动的工作流生成 agent 循环
 * - pause：跑完在途轮（含其全部工具）后不再发起下一轮
 * - stop：中断在途请求，画布保留现状
 * - retry：LLM 层出错时重发同一轮（messages 未被污染）
 */
export function useWorkflowAgent(ctx: WorkflowAgentContext, relayout: () => void) {
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
    if (mutated) relayout()
    status.value = 'done'
    pushLog({ kind: 'system', text: t('workflowAgent.log.done') })
  }

  async function executeToolCalls(toolCalls: ToolCall[]): Promise<boolean> {
    try {
      return await doExecuteToolCalls(toolCalls)
    } finally {
      // 子画布刷新按批合并：一批工具里加 30 个子节点也只重渲染一次
      flushLoopRefresh(ctx.getLf())
    }
  }

  async function doExecuteToolCalls(toolCalls: ToolCall[]): Promise<boolean> {
    let mutated = false
    for (const call of toolCalls) {
      if (status.value === 'stopped') return mutated
      const name = call.function.name
      const log = toolLogFor(call.id, name)
      let args: Record<string, any> = {}
      let result: any
      try {
        args = call.function.arguments ? JSON.parse(call.function.arguments) : {}
        log.text = truncate(JSON.stringify(args), ARGS_SUMMARY_LIMIT)
        if (name === 'plan') {
          // 规划工具：更新面板 todolist，不触发画布操作
          result = applyPlan(args)
          log.text = truncate(
            plan.value.map((item) => `${item.status === 'done' ? '✓' : '○'} ${item.text}`).join(' / '),
            ARGS_SUMMARY_LIMIT
          )
        } else {
          const executor = toolExecutors[name]
          if (!executor) throw new Error(`未知工具: ${name}`)
          result = await executor.execute(args, ctx)
          // skipLayout：坐标已就位（如清空画布），不触发 dagre 重排
          if (executor.mutating && !executor.skipLayout) mutated = true
        }
        log.status = 'success'
      } catch (e: any) {
        result = { error: e?.message ?? String(e) }
        log.status = 'error'
        log.text = `${log.text} → ${result.error}`
      }
      messages.push({
        role: 'tool',
        tool_call_id: call.id,
        // get_workflow / get_node_detail 等大结果工具有更高的截断上限（executor.resultLimit）
        content: truncate(
          JSON.stringify(result ?? null),
          toolExecutors[name]?.resultLimit ?? TOOL_RESULT_LIMIT
        )
      })
      if (name === 'finish' && !result?.error) {
        markDone(args.summary ?? '', mutated)
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
      if (++iteration.value > MAX_ITERATIONS) {
        // 达到轮次上限不算失败：自动暂停，用户点「继续」后重置计数接着跑
        iteration.value = MAX_ITERATIONS
        status.value = 'paused'
        pushLog({ kind: 'system', text: t('workflowAgent.log.maxIterations') })
        return
      }
      abortController = new AbortController()
      narrationLog = null
      let resp
      try {
        // 每轮由前端按画布状态决定传哪些工具（如 clear_workflow 仅画布非空时给出）
        resp = await chatCompletionStream(
          modelId,
          { messages, tools: buildActiveToolSchemas(ctx.getLf()) },
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
      if (mutated && busy.value) relayout()
      if ((status.value as AgentStatus) === 'paused') {
        pushLog({ kind: 'system', text: t('workflowAgent.log.paused') })
        return
      }
    }
  }

  /**
   * 用户消息自动附带画布结构快照：修改场景不能依赖模型自觉调 get_workflow，
   * 每次用户发话都强制给它最新拓扑（节点/连线/分支/循环），完整配置由它按需 get_node_detail
   */
  function withCanvasSnapshot(content: string): string {
    const lf = ctx.getLf()
    if (!hasCanvasContent(lf)) return content
    const snapshot = buildWorkflowSnapshot(lf)
    if (!snapshot) return content
    return (
      `${content}\n\n---\n` +
      `[系统附注] 当前画布结构快照（自动附带，仅含拓扑与输出字段；节点完整配置用 get_node_detail 查询）：\n` +
      snapshot
    )
  }

  function start(requirement: string, model: string) {
    // 画布保持原样：增量修改还是 clear_workflow 清空重建由 AI 自行决策
    modelId = model
    messages = [
      { role: 'system', content: buildSystemPrompt() },
      { role: 'user', content: withCanvasSnapshot(requirement) }
    ]
    logs.value = []
    summary.value = ''
    plan.value = []
    iteration.value = 0
    logSeq = 0
    status.value = 'running'
    pushLog({ kind: 'system', text: t('workflowAgent.log.started') })
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
  function send(message: string) {
    if (status.value !== 'done' && status.value !== 'paused') return
    const content = message.trim()
    if (!content) return
    // 日志只展示用户原话，快照仅进对话上下文
    messages.push({ role: 'user', content: withCanvasSnapshot(content) })
    pushLog({ kind: 'user', text: content })
    iteration.value = 0
    status.value = 'running'
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
