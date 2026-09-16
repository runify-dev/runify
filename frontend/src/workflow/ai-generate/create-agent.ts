import {chatCompletion, toToolSchemas, logDebug, clipForLog} from './common'
import type {
  Call,
  StreamItem,
  ChatCompletionResult,
  AgentEvent,
  AgentPayload,
  AgentTool,
  Executor,
  RunnableAgent,
  AgentResult,
  AgentHandle
} from './type'

// 事件类型的权威定义在 type.ts（对外契约），消费方（panel 等）直接从 type.ts 引

/** 工具结果回喂模型的截断上限（超出截断，避免上下文爆） */
const TOOL_RESULT_LIMIT = 8000
/** tool_end 事件里结果摘要的展示上限 */
const RESULT_SUMMARY_LIMIT = 200

function truncate(text: string, limit: number): string {
  return text.length > limit ? text.slice(0, limit) + '…(truncated)' : text
}

/** 把工具返回值统一包成执行器：已是 Executor 则透传，否则值/Promise 包成不可取消的 Executor */
function toExecutor(ret: any): Executor {
  if (ret && typeof ret === 'object' && typeof ret.cancel === 'function' && ret.promise instanceof Promise) {
    return ret as Executor
  }
  return {
    promise: Promise.resolve(ret), cancel: () => {
    }
  }
}

export function createAgent(agent: RunnableAgent): AgentHandle {
  const {tools, modelId, call, position, maxIterations} = agent

  const controller = new AbortController()
  const signal = controller.signal
  const stopFns: Array<() => void> = []
  const onStop = (fn: () => void) => stopFns.push(fn)
  const stop = () => {
    controller.abort()
    stopFns.forEach((fn) => fn())
  }

  // 跨轮消息历史：useAgentConfig 已建好（system 头 + 传入的初始历史）并挂在 bindings.messages 上（活引用），
  // 这里直接在其上追加每轮 user/assistant/tool；委派工具透传给子的正是这条历史。缺省兜底一条 system。
  const messages: any[] = agent.messages ?? [{role: 'system', content: agent.prompt}]

  const emit = (payload: AgentPayload) => call.onNext({...payload, position})

  const finish = (result: AgentResult, error?: Error): AgentResult => {
    // data 可能是结构化对象（终止工具交付）或文本（隐式结束）；日志/error 事件都要一段可读文本
    const dataText = typeof result.data === 'string' ? result.data : JSON.stringify(result.data ?? '')
    logDebug({
      kind: 'end',
      pos: position,
      status: result.status,
      data: clipForLog(dataText, 800)
    })
    if (result.status === 'error') emit({type: 'error', message: dataText})
    else if (result.status === 'done') emit({type: 'done', data: result.data})
    call.onComplete?.(error)
    return result
  }

  async function run(requirement: string): Promise<AgentResult> {
    if (!modelId) return finish({status: 'error', data: '缺少 modelId'})
    logDebug({kind: 'run', pos: position, requirement: clipForLog(requirement, 800)})
    messages.push({role: 'user', content: requirement})

    try {
      let iteration = 0
      while (true) {
        if (signal.aborted) return finish({status: 'stopped'})
        if (++iteration > maxIterations) {
          return finish({status: 'error', data: `超过最大轮次 ${maxIterations}`})
        }

        const resp = await runTurn(modelId, messages, tools, emit, signal)
        const toolCalls = resp.toolCalls ?? []

        messages.push({
          role: 'assistant',
          content: resp.content || '',
          // thinking 模型要求思考内容按原字段名回传，否则下一轮 400
          ...(resp.reasoningKey && resp.reasoningContent
            ? {[resp.reasoningKey]: resp.reasoningContent}
            : {}),
          ...(toolCalls.length ? {tool_calls: toolCalls} : {})
        })

        // 自然停：模型不再出工具，本段正文即交付（顶层父 agent 的这段文本即给用户看的总结）
        if (!toolCalls.length) {
          return finish({status: 'done', data: resp.content || ''})
        }
        logDebug({
          kind: 'turn',
          pos: position,
          iteration,
          say: clipForLog(resp.content, 500),
          calls: toolCalls.map((tc) => tc.function.name)
        })

        // 本轮工具整批并发执行。同一轮里 AI 是「未见任何结果」就一次性发出这些调用的，故它们之间不可能有
        // 返回值依赖（要用某调用返回的 id/字段，只能等下一轮再发）；而各 apply 里对画布的实际改动都是同步
        // 完成、不跨 await，JS 单线程下并发也不撕裂共享结构。故一轮工具可安全并行，省掉逐个 await 的串行等待。
        // runToolCall 内部吞错（失败转成 { error } 结果），故 Promise.all 不会因某个失败而牵连其余。
        const results = await Promise.all(toolCalls.map((tc) => runToolCall(tc, tools, emit, onStop)))
        if (signal.aborted) return finish({status: 'stopped'})
        // 终止工具短路：某工具返回 { status:'stop', data } 即"交卷"——把 data 作为本 agent 交付收尾，
        // 不再进下一轮（stop 只活在这层，AgentResult 记 done，故上抛给父时不会误停父）。
        const stopped = results.find((r) => r && r.status === 'stop')
        if (stopped) return finish({status: 'done', data: stopped.data})
        // 按原顺序回填 tool 结果（与本轮 assistant.tool_calls 一一对应），逐个按工具自定义上限截断
        for (let k = 0; k < toolCalls.length; k++) {
          const tc = toolCalls[k]
          const limit = tools.find((t) => t.name === tc.function.name)?.resultLimit ?? TOOL_RESULT_LIMIT
          // 调试日志：本 agent(pos) 这一步调了什么工具、入参、结果——层级+建了什么一目了然
          let parsedArgs: any
          try {
            parsedArgs = tc.function.arguments ? JSON.parse(tc.function.arguments) : {}
          } catch {
            parsedArgs = tc.function.arguments
          }
          logDebug({
            kind: 'call',
            pos: position,
            name: tc.function.name,
            args: clipForLog(parsedArgs),
            result: clipForLog(results[k])
          })
          messages.push({
            role: 'tool',
            tool_call_id: tc.id,
            content: truncate(JSON.stringify(results[k] ?? null), limit)
          })
        }
      }
    } catch (error: any) {
      if (signal.aborted) return finish({status: 'stopped'})
      const err = error instanceof Error ? error : new Error(String(error))
      return finish({status: 'error', data: err.message}, err)
    }
  }

  return {run, stop, onStop}
}

/**
 * 跑一轮 LLM 补全：把 SSE 流式条目翻成事件下发（narration/reasoning/tool_*），
 * 并捕获本轮权威 completion 结果（累积后的完整 toolCalls）返回。
 */
function runTurn(
  modelId: string,
  messages: any[],
  tools: AgentTool[],
  emit: (payload: AgentPayload) => void,
  signal: AbortSignal
): Promise<ChatCompletionResult> {
  return new Promise((resolve, reject) => {
    let result: ChatCompletionResult | null = null
    const seenTool = new Set<string>()
    const sink: Call<StreamItem> = {
      onNext: (item) => {
        switch (item.type) {
          case 'delta':
            if (item.content) emit({type: 'narration', delta: item.content})
            break
          case 'reasoning':
            if (item.content) emit({type: 'reasoning', delta: item.content})
            break
          case 'tool_call':
            if (!seenTool.has(item.id)) {
              seenTool.add(item.id)
              emit({type: 'tool_start', id: item.id, name: item.name})
            }
            if (item.arguments) emit({type: 'tool_args', id: item.id, delta: item.arguments})
            break
          case 'completion':
            result = item.result
            break
        }
      },
      onComplete: (error) => {
        if (error) reject(error)
        else if (result) resolve(result)
        else reject(new Error('本轮结束但未收到 completion'))
      }
    }
    chatCompletion(modelId, {messages, tools: toToolSchemas(tools)}, sink, signal)
  })
}

/**
 * 执行单个工具调用：按名找工具 → apply(args)（框架零注入）→ 把返回值包成 Executor，
 * 其 cancel 登记进本 agent 的 onStop（父 stop 时级联取消委派中的子 agent），await 结果。
 * 错误不抛：转成 { error } 结果让模型自行纠偏（工具失败不该整轮崩）。
 */
async function runToolCall(
  tc: { id: string; function: { name: string; arguments: string } },
  tools: AgentTool[],
  emit: (payload: AgentPayload) => void,
  onStop: (fn: () => void) => void
): Promise<any> {
  const name = tc.function.name
  try {
    const args: Record<string, any> = tc.function.arguments ? JSON.parse(tc.function.arguments) : {}
    const tool = tools.find((t) => t.name === name)
    if (!tool) throw new Error(`未知工具: ${name}`)

    const exec = toExecutor(tool.apply(args))
    onStop(exec.cancel)
    const result = await exec.promise
    emit({
      type: 'tool_end',
      id: tc.id,
      ok: true,
      result: truncate(JSON.stringify(result ?? null), RESULT_SUMMARY_LIMIT)
    })
    return result
  } catch (e: any) {
    const message = e?.message ?? String(e)
    emit({type: 'tool_end', id: tc.id, ok: false, result: message})
    return {error: message}
  }
}
