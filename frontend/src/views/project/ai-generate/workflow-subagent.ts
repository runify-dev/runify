import { watch, nextTick } from 'vue'
import { cloneDeep } from 'lodash'
import processorAPI from '@/api/processor'
import { baseWorkflow, WorkflowType } from '@/workflow/common/data'
import { getProfile } from '@/workflow/ai-generate/profiles'
import { useWorkflowAgent } from '@/workflow/ai-generate/useWorkflowAgent'
import { toolExecutors } from '@/workflow/ai-generate/tools'
import type { WorkflowAgentContext } from '@/workflow/ai-generate/types'

/** 页面上真实挂载的 <Workflow> 画布（workflow/index.vue defineExpose 的子集） */
export interface WorkflowHost {
  getLf: () => any
  render: (data?: any) => void
  getGraphData: () => any
  autoLayout: () => void
  validateWorkflow: WorkflowAgentContext['validateWorkflow']
}

/** 子 agent 运行句柄：暴露给页面做嵌套展示与暂停/停止级联 */
export interface SubAgentHandle {
  processorId: string
  processorName: string
  processor: any
  agent: ReturnType<typeof useWorkflowAgent>
}

export interface SubAgentResult {
  valid: boolean
  summary: string
  /** 校验未通过时的错误详情（validate_workflow 工具同构，供外层 agent 决策修复） */
  validation?: any
  /** 终止/失败原因（stopped / error 终态） */
  error?: string
}

/**
 * 「工作流生成」子 agent：把处理器画布 agent 包成一个可 await 的函数。
 * 外层项目 agent 经 generate_workflow 工具调用；每次调用都是全新对话——
 * 画布保留上次成果（快照自动附带），失败重试即天然的增量修复。
 *
 * 流程：读取处理器 → 渲染其工作流到宿主画布 → 自治模式跑处理器画布 agent →
 * 终态后校验（validate_workflow 工具同构）→ 无论是否通过都持久化画布保住进度。
 */
export async function generateProcessorWorkflow(options: {
  projectId: string
  processorId: string
  requirement: string
  modelId: string
  host: WorkflowHost
  /** 子 agent 创建后回调句柄（页面据此嵌套渲染进度）；结束时不清空，保留最后一次的日志 */
  onHandle?: (handle: SubAgentHandle) => void
}): Promise<SubAgentResult> {
  const { projectId, processorId, requirement, modelId, host } = options

  const processor = (await processorAPI.getProcessor(projectId, processorId)).data
  host.render(processor.workflow?.nodes ? processor.workflow : cloneDeep(baseWorkflow))
  await nextTick()

  const ctx: WorkflowAgentContext = {
    getLf: host.getLf,
    profile: getProfile(WorkflowType.PROCESSOR),
    getDetails: () => processor,
    validateWorkflow: host.validateWorkflow
  }
  const agent = useWorkflowAgent(ctx, host.autoLayout, { autonomous: true })
  options.onHandle?.({ processorId, processorName: processor.name, processor, agent })

  agent.start(requirement, modelId)
  const terminal = await new Promise<string>((resolve) => {
    const stopWatch = watch(agent.status, (value) => {
      if (value === 'done' || value === 'error' || value === 'stopped') {
        stopWatch()
        resolve(value)
      }
    })
  })

  if (terminal === 'stopped') {
    return { valid: false, summary: '', error: '生成被终止' }
  }
  if (terminal === 'error') {
    const lastError = [...agent.logs.value].reverse().find((log) => log.kind === 'error')
    return { valid: false, summary: '', error: lastError?.text ?? '生成失败' }
  }

  // 校验与 validate_workflow 工具完全同构（节点配置 + 结构 lint + 开始节点归类）
  const validation = await toolExecutors.validate_workflow.execute({}, ctx)
  // 无论是否通过都保存画布：进度不丢，失败后重调 generate_workflow 可增量修复
  await processorAPI.editProcessor(projectId, processorId, { workflow: host.getGraphData() })
  return {
    valid: validation.valid === true,
    summary: agent.summary.value,
    ...(validation.valid === true ? {} : { validation })
  }
}
