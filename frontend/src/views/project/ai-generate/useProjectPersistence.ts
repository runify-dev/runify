import { ref } from 'vue'
import type { AgentCheckpoint, LogItem, PlanItem, PendingAsk } from '@/workflow/ai-generate/useAgentLoop'
import { projectAiApi } from './persistence-api'

/**
 * 会话持久化层：把循环的检查点快照落到后端（会话 upsert + 消息 append-only）。
 * best-effort——写失败只告警不阻塞生成；内部串行队列保证消息 seq 顺序与「先建会话后追加」。
 *
 * 会话正文分两处存：
 * - message 表：OpenAI messages（去 system，续跑的真源）
 * - session.timeline：UI 日志（整读整画恢复对话面板）；session.facts：{ pendingAsk, plan }
 */
export function useProjectPersistence(getProjectId: () => string) {
  const sessionId = ref<string | null>(null)
  /** 已落库的 loop 消息数（含 system 占位，checkpoint 按此取增量） */
  let persistedCount = 0
  /** 串行队列：会话创建 → 消息追加 → 状态更新 顺序不乱 */
  let chain: Promise<any> = Promise.resolve()

  function enqueue(task: () => Promise<any>) {
    chain = chain.then(task).catch((e) => {
      // 持久化是尽力而为：失败仅告警，不影响前端生成流程
      console.warn('[project-ai persistence]', e)
    })
    return chain
  }

  function firstUserTitle(logs: LogItem[]): string {
    const text = logs.find((log) => log.kind === 'user')?.text?.trim() || ''
    return text.length > 60 ? text.slice(0, 60) : text
  }

  async function ensureSession(title: string) {
    if (sessionId.value) return
    const res = await projectAiApi.createSession(getProjectId(), { title, status: 'running' })
    sessionId.value = res.data?.id ?? null
  }

  /**
   * 循环检查点回调：同步算增量与冻结状态，再入队异步落库（避免异步读到后续变更导致重复）。
   */
  function onCheckpoint(snapshot: AgentCheckpoint) {
    const pending = snapshot.messages.slice(persistedCount).filter((m) => m.role !== 'system')
    persistedCount = snapshot.messages.length
    const title = firstUserTitle(snapshot.logs)
    const state = {
      status: snapshot.status,
      summary: snapshot.summary,
      timeline: [...snapshot.logs],
      facts: {
        pendingAsk: snapshot.pendingAsk,
        plan: [...snapshot.plan],
        modelId: snapshot.modelId
      } as any
    }
    enqueue(async () => {
      await ensureSession(title)
      if (!sessionId.value) return
      const pid = getProjectId()
      const sid = sessionId.value
      for (const payload of pending) {
        await projectAiApi.appendMessage(pid, { ownerType: 'session', ownerId: sid, payload })
      }
      await projectAiApi.updateSession(pid, sid, {
        status: state.status,
        summary: state.summary,
        timeline: state.timeline,
        facts: state.facts
      })
    })
  }

  /** 起手新会话：清空会话上下文，下一次 checkpoint 惰性创建 */
  function newSession() {
    sessionId.value = null
    persistedCount = 0
    chain = Promise.resolve()
  }

  /** 恢复既有会话：绑定 id，并告知已落库的消息数（system + 已存条数），后续增量在此之上 */
  function bindSession(id: string, alreadyPersisted: number) {
    sessionId.value = id
    persistedCount = alreadyPersisted
    chain = Promise.resolve()
  }

  return {
    sessionId,
    onCheckpoint,
    newSession,
    bindSession,
    // 供页面直接调用的读/删
    listSessions: () => projectAiApi.listSessions(getProjectId()),
    getSession: (id: string) => projectAiApi.getSession(getProjectId(), id),
    listSessionMessages: (id: string) =>
      projectAiApi.listMessages(getProjectId(), 'session', id),
    deleteSession: (id: string) => projectAiApi.deleteSession(getProjectId(), id)
  }
}

/** 从会话实体 + 消息流组装 restore 入参（modelId 从 facts 取回，续跑沿用原模型） */
export function buildRestoreState(
  session: any,
  messages: any[]
): {
  modelId: string
  messages: any[]
  logs: LogItem[]
  plan: PlanItem[]
  summary: string
  status: any
  pendingAsk: PendingAsk | null
} {
  const facts = session?.facts ?? {}
  return {
    modelId: facts.modelId ?? '',
    messages: messages.map((m) => m.payload).filter(Boolean),
    logs: (session?.timeline ?? []) as LogItem[],
    plan: (facts.plan ?? []) as PlanItem[],
    summary: session?.summary ?? '',
    status: session?.status ?? 'paused',
    pendingAsk: (facts.pendingAsk ?? null) as PendingAsk | null
  }
}
