import axios from 'axios'
import processorAPI from '@/api/processor'
import { toolSchemas } from '@/workflow/ai-generate/tools'
import type { ToolCallMeta } from '@/workflow/ai-generate/useAgentLoop'
import {
  processorToolSchemas,
  processorToolExecutors
} from '@/workflow/ai-generate/profiles/processor-http/tools'
import { projectAiApi } from './persistence-api'
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
  /** 当前项目规范（update_blueprint 维护，自动注入每个子代理需求）；由 useProjectAgent 注入 */
  getConventions(): string
  setConventions(text: string): void
  /** 项目部署前缀 project.path（部署 URL = path + 处理器路径）；由 useProjectAgent 注入 */
  getProjectPath(): string
}

/** index.vue 传入的部分（规范/前缀存取由 useProjectAgent 补齐） */
export type ProjectAgentInput = Omit<
  ProjectAgentContext,
  'getConventions' | 'setConventions' | 'getProjectPath'
>

/**
 * 项目部署前缀说明：部署 URL = project.path + 处理器路径，页面型端点必须用 <base href> 承接前缀，
 * 否则页面里的 fetch 走绝对根路径会漏掉前缀而 404。仅在 path 是非平凡前缀时才需要。
 */
export function buildDeploymentNote(path: string): string {
  const base = path.endsWith('/') ? path : path + '/'
  return (
    `[项目部署信息] 本项目部署前缀为 "${path}"：处理器路径如 /api/x 实际对外地址是 "${path}/api/x"。\n` +
    `页面型端点（返回 HTML）必须处理前缀，否则页面 fetch 会漏掉前缀而 404：\n` +
    `- <head> 内加 <base href="${base}">；\n` +
    `- 页面里所有 fetch / 超链接 / 静态资源用相对路径（去掉开头的 "/"，如 fetch("api/x") 而非 fetch("/api/x")），` +
    `这样才会正确解析到 "${path}/api/x"。`
  )
}

/** path 是否为需要 base href 承接的非平凡前缀（根路径 "" / "/" 无需处理） */
function hasPathPrefix(path: string): boolean {
  return !!path && path !== '/'
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

  update_blueprint: {
    execute: async ({ description, conventions }, ctx) => {
      if (description === undefined && conventions === undefined) {
        throw new Error('description / conventions 至少传一个')
      }
      await projectAiApi.upsertBlueprint(ctx.getProjectId(), {
        ...(description !== undefined ? { description: String(description) } : {}),
        ...(conventions !== undefined ? { conventions: String(conventions) } : {})
      })
      // 立即生效：本地规范同步更新，后续 generate_workflow 与用户消息装饰即读到新版
      if (conventions !== undefined) ctx.setConventions(String(conventions))
      return { ok: true }
    }
  },

  generate_workflow: {
    resultLimit: 4000,
    execute: async ({ processorId, requirement }, ctx, meta) => {
      if (!processorId) throw new Error('processorId 不能为空')
      if (!requirement || !String(requirement).trim()) throw new Error('requirement 不能为空')
      // 自动把项目规范 + 部署前缀前置进子代理需求——全项目统一，AI 不必在每个 requirement 里重述
      const conventions = ctx.getConventions()
      const projectPath = ctx.getProjectPath()
      const parts: string[] = []
      if (conventions) parts.push(`【项目规范（全项目统一遵守）】\n${conventions}`)
      if (hasPathPrefix(projectPath)) parts.push(buildDeploymentNote(projectPath))
      parts.push(`【本端点需求】\n${String(requirement)}`)
      return await ctx.generateWorkflow({
        processorId,
        requirement: parts.join('\n\n'),
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

  undeploy_processor: {
    execute: async ({ processorId }, ctx) => {
      if (!processorId) throw new Error('processorId 不能为空')
      const res = await processorAPI.undeploy(ctx.getProjectId(), processorId)
      return { ok: true, isDeploy: res.data?.isDeploy === true }
    }
  },

  delete_processor: {
    execute: async ({ processorId }, ctx) => {
      if (!processorId) throw new Error('processorId 不能为空')
      // 后端会先下线端点再删实体
      await processorAPI.deleteProcessor(ctx.getProjectId(), processorId)
      return { ok: true }
    }
  },

  execute_processor: {
    resultLimit: 4000,
    // 调用一次已部署的处理器端点（复刻 UI「执行」：axios 打 project.path + meta.path）
    execute: async ({ processorId, params, body }, ctx) => {
      if (!processorId) throw new Error('processorId 不能为空')
      const processor = (await processorAPI.getProcessor(ctx.getProjectId(), processorId)).data
      const meta = processor?.meta
      if (!meta?.method || !meta?.path) {
        throw new Error('该处理器未配置 HTTP 入参（method/path），无法执行')
      }
      const query = params && typeof params === 'object' ? params : {}
      // 路径参数 :field 用 params 里的同名值替换
      let path = String(meta.path)
      ;(meta.parameters ?? []).forEach((param: any) => {
        if (param.location === 'path' && query[param.field] != null) {
          path = path.replace(`:${param.field}`, encodeURIComponent(String(query[param.field])))
        }
      })
      const method = String(meta.method).toUpperCase()
      const config: any = { method: method.toLowerCase(), url: `${ctx.getProjectPath()}${path}` }
      if (method === 'POST' || method === 'PUT') {
        config.data = body ?? {}
        config.headers = {
          'Content-Type':
            meta.contentType === 'multipart/form-data' ? 'multipart/form-data' : 'application/json'
        }
      } else {
        config.params = query
      }
      try {
        const res = await axios(config)
        return { ok: true, status: res.status, data: res.data }
      } catch (e: any) {
        // 未部署会 404——把状态与响应体回给模型，让它先 deploy 或修错
        return { ok: false, status: e?.response?.status, data: e?.response?.data ?? e?.message }
      }
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
  {
    type: 'function',
    function: {
      name: 'ask_user',
      description:
        '向用户提问并等待回答（会挂起生成直到用户回复）。仅在无法自行决策时使用：' +
        '需求有歧义、缺少必要信息（如未指定数据来源/字段口径）、需要用户在多个方案间拍板、' +
        '或所需资源不存在（无数据库连接池、无对应数据表）。禁止用它询问可自行合理决定的细节。' +
        '提问要具体，能给候选项就给 options（用户可点选或自由输入）。',
      parameters: {
        type: 'object',
        properties: {
          question: { type: 'string', description: '向用户提出的问题（中文，具体清晰）' },
          options: {
            type: 'array',
            items: { type: 'string' },
            description: '可选的候选答案（可空）；提供时用户可一键点选，也可自由输入'
          }
        },
        required: ['question']
      }
    }
  },
  ...processorToolSchemas,
  {
    type: 'function',
    function: {
      name: 'update_blueprint',
      description:
        '建立或更新【项目规范】——全项目统一遵守的工程约定。新项目开工前必须先调用它把规范列清，' +
        '典型内容：统一响应信封（如 {code,message,data}）、路径前缀承接方式、错误响应格式、命名规范、' +
        '通用规则（如"每个接口必须有 desc 描述"）、默认数据源。规范会自动注入每个 generate_workflow ' +
        '的子代理，无需在各 requirement 里重复；需求变化时再次调用全量覆盖。',
      parameters: {
        type: 'object',
        properties: {
          conventions: {
            type: 'string',
            description: '项目规范全文（markdown，全量覆盖）。逐条列清响应信封/前缀/错误格式/命名/通用规则'
          },
          description: { type: 'string', description: '可选：项目简报（这是个什么项目、包含哪些端点）' }
        }
      }
    }
  },
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
      name: 'undeploy_processor',
      description: '下线处理器，使其端点不再对外暴露（处理器本身保留）。一次性初始化处理器（如建表）执行完后用它清理。',
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
      name: 'delete_processor',
      description:
        '彻底删除一个处理器（先下线端点再删实体，不可恢复）。用于清理一次性初始化处理器（如建表跑完后删掉），' +
        '或删除确实不需要的端点。删除业务端点前应向用户确认。',
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
      name: 'execute_processor',
      description:
        '执行（调用）一个【已部署】的处理器端点一次并返回其响应。用于一次性初始化 —— 如建表处理器部署后跑一次真正建表。' +
        '必须先 deploy_processor，否则返回 404。会产生真实副作用（写库/建表等），只对你已向用户说明的初始化任务调用。' +
        'params 传查询/路径参数，body 传请求体（按该处理器的 HTTP 契约）。返回 { ok, status, data }。',
      parameters: {
        type: 'object',
        properties: {
          processorId: { type: 'string', description: '处理器 id' },
          params: { type: 'object', description: '查询参数 / 路径参数（键为参数名）；无则省略' },
          body: { type: 'object', description: '请求体（POST/PUT 时按契约传）；无则省略' }
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
