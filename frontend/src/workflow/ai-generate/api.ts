import { postStream } from '@/request/admin/index'

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

export interface ChatCompletionCallbacks {
  onDelta?: (text: string) => void
  onReasoning?: (text: string) => void
  onToolCallStart?: (info: { id: string; name: string }) => void
}

export interface SseEvent {
  type: 'delta' | 'reasoning' | 'tool_call_start' | 'completion' | 'error'
  [key: string]: any
}

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
 * 调用后端聊天补全代理（SSE），返回本轮的最终 completion
 * delta/reasoning/tool_call_start 通过回调实时上报
 */
export async function chatCompletionStream(
  modelId: string,
  payload: { messages: any[]; tools?: any[] },
  signal: AbortSignal,
  callbacks: ChatCompletionCallbacks = {}
): Promise<ChatCompletionResult> {
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
  let completion: ChatCompletionResult | null = null

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
          callbacks.onDelta?.(event.content ?? '')
          break
        case 'reasoning':
          callbacks.onReasoning?.(event.content ?? '')
          break
        case 'tool_call_start':
          callbacks.onToolCallStart?.({ id: event.id, name: event.name })
          break
        case 'completion':
          completion = {
            content: event.content ?? '',
            toolCalls: event.toolCalls ?? [],
            finishReason: event.finishReason ?? null,
            usage: event.usage ?? null,
            reasoningKey: event.reasoningKey ?? null,
            reasoningContent: event.reasoningContent ?? null
          }
          break
        case 'error':
          throw new Error(event.message ?? 'LLM stream error')
      }
    }
  }

  if (!completion) {
    throw new Error('Stream ended without completion event')
  }
  return completion
}
