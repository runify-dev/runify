import processorAPI from '@/api/processor'
import { toolSchemas } from '@/workflow/ai-generate/tools'
import type { ToolCallMeta } from '@/workflow/ai-generate/useAgentLoop'
import {
  processorToolSchemas,
  processorToolExecutors
} from '@/workflow/ai-generate/profiles/processor-http/tools'
import type { SubAgentResult } from './workflow-subagent'

/**
 * 项目级 agent 的工具集：全部是纯 API 调用与子代理调度，不直接操作画布。
 * 数据库资源查询三件套直接复用处理器画布的专属工具（它们本就不依赖画布上下文）。
 */

export interface ProjectAgentContext {
  /** 页面路由可能在项目间切换，取值时刻读取 */
  getProjectId(): string
  /**
   * generate_workflow 的实际执行者（页面提供：驱动子 代理跑宿主画布）。
   * callId 用于把内联画布卡片锚定到触发它的那次 tool_call。
   */
  generateWorkflow(args: {
    processorId: string
    requirement: string
    callId?: string
  }): Promise<SubAgentResult>
}

export interface ProjectToolExecution {
  resultLimit?: number
  execute(args: Record<string, any>, ctx: ProjectAgentContext, meta?: ToolCallMeta): Promise<any> | any
}

/** 复用处理器画布的数据库工具（执行器不读画布上下文，传空即可） */
const reuseDbTool = (name: string): ProjectToolExecution => ({
  resultLimit: processorToolExecutors[name].resultLimit,
  execute: (args) => processorToolExecutors[name].execute(args, {} as any)
})

export const projectToolExecutors: Record<string, ProjectToolExecution> = {
  get_database_pools: reuseDbTool('get_database_pools'),
  get_database_tables: reuseDbTool('get_database_tables'),
  get_database_columns: reuseDbTool('get_database_columns'),

  list_processors: {
    execute: async (_args, ctx) => {
      const res = await processorAPI.pageProcessor(ctx.getProjectId(), {
        currentPage: 1,
        pageSize: 200
      })
      return (res.data?.records ?? []).map((item: any) => ({
        id: item.id,
        name: item.name,
        desc: item.desc,
        isDeploy: item.isDeploy === true,
        method: item.meta?.method ?? null,
        path: item.meta?.path ?? null
      }))
    }
  },

  create_processor: {
    execute: async ({ name, desc }, ctx) => {
      if (!name || !String(name).trim()) throw new Error('name 不能为空')
      const res = await processorAPI.createProcessor(ctx.getProjectId(), {
        name: String(name).trim(),
        desc: desc ? String(desc) : '',
        protocol: 'HTTP'
      })
      return { processorId: res.data.id, name: res.data.name }
    }
  },

  update_processor: {
    execute: async ({ processorId, name, desc }, ctx) => {
      if (!processorId) throw new Error('processorId 不能为空')
      if (name === undefined && desc === undefined) throw new Error('name/desc 至少传一个')
      await processorAPI.editProcessor(ctx.getProjectId(), processorId, {
        ...(name !== undefined ? { name } : {}),
        ...(desc !== undefined ? { desc } : {})
      })
      return { ok: true }
    }
  },

  generate_workflow: {
    resultLimit: 4000,
    execute: async ({ processorId, requirement }, ctx, meta) => {
      if (!processorId) throw new Error('processorId 不能为空')
      if (!requirement || !String(requirement).trim()) throw new Error('requirement 不能为空')
      return await ctx.generateWorkflow({
        processorId,
        requirement: String(requirement),
        callId: meta?.callId
      })
    }
  },

  deploy_processor: {
    execute: async ({ processorId }, ctx) => {
      if (!processorId) throw new Error('processorId 不能为空')
      const res = await processorAPI.deploy(ctx.getProjectId(), processorId)
      return { ok: true, isDeploy: res.data?.isDeploy === true }
    }
  },

  finish: {
    execute: ({ summary }) => ({ ok: true, summary: summary ?? '' })
  }
}

/** plan 工具与画布 agent 完全同构，schema 直接复用 */
const planSchema = toolSchemas.find((tool: any) => tool.function.name === 'plan')!

export const projectToolSchemas = [
  planSchema,
  ...processorToolSchemas,
  {
    type: 'function',
    function: {
      name: 'list_processors',
      description:
        '获取当前项目的处理器清单。返回元素 { id, name, desc, isDeploy 是否已部署, method, path }；' +
        'method/path 为 null 说明该处理器的 HTTP 入参尚未配置（工作流未生成或未配置开始节点）。',
      parameters: { type: 'object', properties: {} }
    }
  },
  {
    type: 'function',
    function: {
      name: 'create_processor',
      description:
        '创建一个 HTTP 处理器（一个端点一个处理器），返回 processorId。' +
        '只建壳不含工作流——创建后必须用 generate_workflow 生成其工作流。',
      parameters: {
        type: 'object',
        properties: {
          name: { type: 'string', description: '处理器名称（简洁中文，如「查询文章列表」）' },
          desc: {
            type: 'string',
            description: '一句话职责说明，含方法与路径（如「查询文章列表 GET /api/posts」）'
          }
        },
        required: ['name']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'update_processor',
      description: '修改处理器的名称或描述（不涉及工作流与 HTTP 入参）。',
      parameters: {
        type: 'object',
        properties: {
          processorId: { type: 'string', description: '处理器 id' },
          name: { type: 'string', description: '新名称' },
          desc: { type: 'string', description: '新描述' }
        },
        required: ['processorId']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'generate_workflow',
      description:
        '调度子代理为指定处理器搭建并保存工作流（耗时较长）。互不依赖的端点可同一轮并行发起多个' +
        '（建议不超过 3 个，超出自动排队）；禁止对同一 processorId 并发调用。' +
        'requirement 必须自包含（子代理对项目一无所知）：项目背景、本端点职责、HTTP 契约' +
        '（方法/路径/参数/请求体）、响应信封与 data 结构、数据依赖（连接池 id/表/列）、关联端点。' +
        '返回 { valid, summary, validation }：valid=false 时按 validation 里的错误重调本工具增量修复' +
        '（画布保留上次成果）。',
      parameters: {
        type: 'object',
        properties: {
          processorId: { type: 'string', description: '处理器 id（create_processor 返回或 list_processors 查询）' },
          requirement: { type: 'string', description: '自包含的工作流需求描述（书写契约见系统提示词）' }
        },
        required: ['processorId', 'requirement']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'deploy_processor',
      description: '部署处理器使其端点生效。仅对 generate_workflow 返回 valid=true 的处理器调用。',
      parameters: {
        type: 'object',
        properties: {
          processorId: { type: 'string', description: '处理器 id' }
        },
        required: ['processorId']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'finish',
      description: '声明项目生成完成并结束。必须在全部端点处理完（生成成功或明确跳过）后调用。',
      parameters: {
        type: 'object',
        properties: {
          summary: {
            type: 'string',
            description: '中文总结：创建了哪些端点（方法/路径/职责）、如何使用、哪些已部署、有无遗留问题'
          }
        },
        required: ['summary']
      }
    }
  }
]
