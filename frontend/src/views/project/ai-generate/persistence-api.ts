import { get, post, put, del } from '@/request/admin/index'

/**
 * 项目级 AI 生成的持久化 API 客户端（对接后端 ProjectAiRoute）。
 * 蓝图 / 会话 / 任务台账 / append-only 消息流。
 */
const base = (projectId: string) => `/project/${projectId}/ai`

export type MessageOwnerType = 'session' | 'task'

export const projectAiApi = {
  // L1 蓝图
  getBlueprint: (projectId: string) => get(`${base(projectId)}/blueprint`, {}),
  upsertBlueprint: (
    projectId: string,
    body: { description?: string; conventions?: string; memory?: any }
  ) => put(`${base(projectId)}/blueprint`, body, {}),

  // L2 会话
  createSession: (projectId: string, body: { title?: string; status?: string }) =>
    post(`${base(projectId)}/session`, body, {}),
  listSessions: (projectId: string) => get(`${base(projectId)}/session`, {}),
  getSession: (projectId: string, sessionId: string) =>
    get(`${base(projectId)}/session/${sessionId}`, {}),
  updateSession: (
    projectId: string,
    sessionId: string,
    body: { title?: string; status?: string; summary?: string; facts?: any; windowFromSeq?: number; timeline?: any }
  ) => put(`${base(projectId)}/session/${sessionId}`, body, {}),
  deleteSession: (projectId: string, sessionId: string) =>
    del(`${base(projectId)}/session/${sessionId}`),

  // L3 任务台账
  createTask: (
    projectId: string,
    sessionId: string,
    body: { processorId?: string; requirement?: string; status?: string }
  ) => post(`${base(projectId)}/session/${sessionId}/task`, body, {}),
  listTasks: (projectId: string, sessionId: string) =>
    get(`${base(projectId)}/session/${sessionId}/task`, {}),
  updateTask: (
    projectId: string,
    taskId: string,
    body: {
      processorId?: string | null
      status?: string
      summary?: string
      facts?: any
      windowFromSeq?: number
      workflow?: any
      result?: any
      timeline?: any
    }
  ) => put(`${base(projectId)}/task/${taskId}`, body, {}),

  // 统一消息流（append-only，seq 由服务端分配）
  appendMessage: (
    projectId: string,
    body: { ownerType: MessageOwnerType; ownerId: string; payload: any; tokenCount?: number }
  ) => post(`${base(projectId)}/message`, body, {}),
  listMessages: (
    projectId: string,
    ownerType: MessageOwnerType,
    ownerId: string,
    fromSeq = 0
  ) => get(`${base(projectId)}/message`, { ownerType, ownerId, fromSeq })
}
