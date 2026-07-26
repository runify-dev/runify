import { ref } from 'vue'
import { TreeCommonAPI } from '@/api/tree'
import { useAgentLoop } from '@/workflow/ai-generate/useAgentLoop'
import { buildProjectPrompt } from './prompt'
import {
  projectToolSchemas,
  projectToolExecutors,
  buildDeploymentNote,
  type ProjectAgentContext,
  type ProjectAgentInput
} from './tools'
import { projectAiApi } from './persistence-api'
import { useProjectPersistence, buildRestoreState } from './useProjectPersistence'

/**
 * 项目级生成 agent = 通用 agent 循环（useAgentLoop）+ 项目运行时 + 会话持久化 + 项目规范。
 * 不操作画布：工具全是纯 API 调用，工作流搭建经 generate_workflow 交给子代理。
 *
 * 项目规范（blueprint.conventions）：会话起手/恢复时读回，注入每次用户消息与每个子代理需求，
 * 全项目统一；update_blueprint 工具维护，就地更新本地副本即刻生效。
 */
export function useProjectAgent(input: ProjectAgentInput) {
  const persistence = useProjectPersistence(input.getProjectId)
  const conventions = ref('')
  const projectPath = ref('')

  // 补齐规范/前缀存取，构成工具用的完整 ctx
  const ctx: ProjectAgentContext = {
    ...input,
    getConventions: () => conventions.value,
    setConventions: (text: string) => {
      conventions.value = text
    },
    getProjectPath: () => projectPath.value
  }

  async function loadBlueprint() {
    try {
      const res = await projectAiApi.getBlueprint(input.getProjectId())
      conventions.value = res.data?.conventions ?? ''
    } catch {
      conventions.value = ''
    }
  }

  // 项目部署前缀 project.path（部署 URL = path + 处理器路径），页面型端点需据此加 <base href>
  async function loadProjectPath() {
    try {
      const res = await new TreeCommonAPI('project').getResource(input.getProjectId())
      projectPath.value = res.data?.path ?? ''
    } catch {
      projectPath.value = ''
    }
  }

  /**
   * 用户消息装饰：附带项目规范 + 处理器清单现状。
   * 规范为空且无处理器 → 视为新项目，提示 AI 先建规范。
   */
  async function decorate(content: string): Promise<string> {
    let result = content
    if (conventions.value) {
      result += `\n\n---\n[项目规范（已建立，全程遵守）]\n${conventions.value}`
    }
    // 部署前缀（非根路径才需处理）：页面型端点据此加 <base href> 承接前缀
    if (projectPath.value && projectPath.value !== '/') {
      result += `\n\n---\n${buildDeploymentNote(projectPath.value)}`
    }
    let processors: any[] = []
    try {
      const list = await projectToolExecutors.list_processors.execute({}, ctx)
      if (Array.isArray(list)) processors = list
    } catch {
      /* 清单接口报错不阻塞发话 */
    }
    if (processors.length) {
      result += `\n\n---\n[当前项目处理器清单（自动附带）]\n${JSON.stringify(processors)}`
    }
    if (!conventions.value && !processors.length) {
      result += `\n\n---\n[系统提示] 这是新项目：请先用 update_blueprint 列清项目规范（统一响应信封等），再规划端点。`
    }
    return result
  }

  const agent = useAgentLoop(
    {
      buildSystemPrompt: buildProjectPrompt,
      buildTools: () => projectToolSchemas,
      resolveTool: (name) => {
        const executor = projectToolExecutors[name]
        if (!executor) return undefined
        return {
          resultLimit: executor.resultLimit,
          execute: (args, meta) => executor.execute(args, ctx, meta)
        }
      },
      decorateUserMessage: decorate
    },
    {
      // 工具全是 API 调用与子代理调度，同一批次并发执行；
      // 并行 generate_workflow 的画布互不相同（页面按 processorId 分卡片），并发上限由页面信号量把关
      parallelTools: true,
      onCheckpoint: persistence.onCheckpoint
    }
  )

  // 起手新会话：清掉持久化上下文并读回项目规范（新项目则为空，AI 会先建）
  const start = async (requirement: string, model: string) => {
    persistence.newSession()
    await Promise.all([loadBlueprint(), loadProjectPath()])
    return agent.start(requirement, model)
  }

  /**
   * 恢复既有会话：读回会话 + 消息流重建循环运行时（悬空 tool_call 补位），并载入项目规范。
   */
  async function resumeSession(id: string): Promise<{ modelId: string }> {
    const [sessionRes, messagesRes] = await Promise.all([
      persistence.getSession(id),
      persistence.listSessionMessages(id)
    ])
    const session = sessionRes.data
    const messages = messagesRes.data ?? []
    const state = buildRestoreState(session, messages)
    agent.restore(state)
    persistence.bindSession(id, 1 + state.messages.length)
    await Promise.all([loadBlueprint(), loadProjectPath()])
    return { modelId: state.modelId }
  }

  return {
    ...agent,
    start,
    sessionId: persistence.sessionId,
    resumeSession,
    listSessions: persistence.listSessions,
    deleteSession: persistence.deleteSession
  }
}
