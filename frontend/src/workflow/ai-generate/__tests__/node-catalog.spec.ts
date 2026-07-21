import { describe, it, expect } from 'vitest'
import { renderDoc, buildNodeProperties, sanitizeNodeDataPatch } from '../node-catalog'
import { getAllProfiles, getProfile } from '../profiles'
import { AGENT_TOOL_DEFINITIONS, AGENT_TOOL_NODE_MAP } from '../agent-tools'
import { WorkflowType } from '@/workflow/common/data'
import { validate as validateProcessorResponse } from '@/workflow/nodes/response-node/content/processor/validator'
import { validate as validateDatabaseSearch } from '@/workflow/nodes/database-search-node/content/validator'

const applicationCatalog = getProfile(WorkflowType.APPLICATION).catalog
const processorCatalog = getProfile(WorkflowType.PROCESSOR).catalog

describe('node-catalog 结构完整性（遍历所有画布档案）', () => {
  it('每个目录条目都能渲染文档且包含输入/输出段', () => {
    for (const profile of getAllProfiles()) {
      for (const entry of Object.values(profile.catalog)) {
        const doc = renderDoc(entry)
        expect(doc, `${profile.workflowType}/${entry.type}`).toContain('输入（nodeData 字段）')
        expect(doc, `${profile.workflowType}/${entry.type}`).toContain('输出字段')
      }
    }
  })

  it('每个可添加条目都能生成默认 properties（含 field_list 与 nodeData）', () => {
    for (const profile of getAllProfiles()) {
      for (const entry of Object.values(profile.catalog)) {
        if (entry.addable === false) continue // 画布固有节点不经 add_node 生成
        const { properties, warnings } = buildNodeProperties(profile.catalog, entry.type, undefined, undefined)
        expect(warnings, `${profile.workflowType}/${entry.type}`).toEqual([])
        expect(properties.nodeData, `${profile.workflowType}/${entry.type}`).toBeTypeOf('object')
        expect(Array.isArray(properties.field_list), `${profile.workflowType}/${entry.type}`).toBe(true)
      }
    }
  })

  it('静态输出节点的 field_list 与 outputs（非隐藏）一致', () => {
    for (const profile of getAllProfiles()) {
      for (const entry of Object.values(profile.catalog)) {
        if (entry.applyFieldList) continue // 动态输出节点跳过
        const { properties } = buildNodeProperties(profile.catalog, entry.type, undefined, undefined)
        const expected = entry.outputs.filter((o) => !o.hidden).map((o) => o.value)
        expect(properties.field_list.map((f: any) => f.value), `${profile.workflowType}/${entry.type}`).toEqual(
          expected
        )
      }
    }
  })

  it('每个画布档案都能生成系统提示词且包含自己的节点目录', () => {
    for (const profile of getAllProfiles()) {
      const prompt = profile.buildSystemPrompt()
      expect(prompt.length, profile.workflowType).toBeGreaterThan(500)
      for (const entry of Object.values(profile.catalog)) {
        expect(prompt, `${profile.workflowType}/${entry.type}`).toContain(entry.type)
      }
    }
  })

  it('未知字段被过滤并产生警告', () => {
    const { patch, warnings } = sanitizeNodeDataPatch(applicationCatalog, 'knowledge-search-node', {
      knowledgeIds: ['a'],
      folderIds: ['b'], // 已废弃的错误字段名
      keywords: 'x' // 拼写错误
    })
    expect(patch).toEqual({ knowledgeIds: ['a'] })
    expect(warnings.length).toBe(1)
    expect(warnings[0]).toContain('folderIds')
    expect(warnings[0]).toContain('keywords')
  })

  it('enum 非法取值产生警告', () => {
    const { warnings } = sanitizeNodeDataPatch(applicationCatalog, 'loop-node', { loopType: 'while' })
    expect(warnings.some((w) => w.includes('loopType'))).toBe(true)
  })

  it('loop 默认初始化 loop-start-node，loopVariables 自动推断 dataType', () => {
    const { properties } = buildNodeProperties(applicationCatalog, 'loop-node', undefined, {
      loopVariables: [{ name: 'context', defaultValue: '[]' }]
    })
    expect(properties.nodeData.children.nodes[0].type).toBe('loop-start-node')
    expect(properties.nodeData.loopVariables[0].dataType).toBe('array')
    expect(properties.field_list.map((f: any) => f.value)).toContain('context')
  })

  it('judge 自动补分支/条件 id 并保证 else 分支', () => {
    const { properties } = buildNodeProperties(applicationCatalog, 'judge-node', undefined, {
      branches: [{ type: 'if', conditions: [{ variable: ['a', 'b'], compare: 'eq', value: '1' }] }]
    })
    const branches = properties.nodeData.branches
    expect(branches.every((b: any) => !!b.id)).toBe(true)
    expect(branches.some((b: any) => b.type === 'else')).toBe(true)
  })

  it('ai-chat 标准工具名自动替换为完整标准定义', () => {
    const { properties } = buildNodeProperties(applicationCatalog, 'ai-chat-node', undefined, {
      modelId: 'm1',
      tools: { location: 'customize', reference: [], tools: [{ type: 'function', function: { name: 'grep' } }] }
    })
    const tool = properties.nodeData.tools.tools[0]
    expect(tool.function.parameters).toBeDefined()
    expect(tool.function.parameters.properties.pattern).toBeDefined()
  })

  it('目录按画布档案隔离：数据库/缓存节点仅处理器目录有', () => {
    const processorOnly = ['database-search-node', 'database-insert-node', 'cache-query-node', 'cache-write-node']
    for (const type of processorOnly) {
      expect(processorCatalog[type], type).toBeDefined()
      expect(applicationCatalog[type], type).toBeUndefined()
    }
    // 共享节点两个目录都有
    for (const type of ['ai-chat-node', 'judge-node', 'loop-node', 'response-node']) {
      expect(applicationCatalog[type], type).toBeDefined()
      expect(processorCatalog[type], type).toBeDefined()
    }
  })

  it('response-node 双画布变体：处理器版含 status/headers 且默认值可过校验', () => {
    const appEntry = applicationCatalog['response-node']
    const processorEntry = processorCatalog['response-node']
    expect(appEntry.inputs.some((i) => i.key === 'status')).toBe(false)
    expect(processorEntry.inputs.some((i) => i.key === 'status')).toBe(true)
    expect(processorEntry.inputs.some((i) => i.key === 'headers')).toBe(true)

    // 处理器版默认 nodeData 必须满足其校验器（status 必填）
    const { properties } = buildNodeProperties(processorCatalog, 'response-node', undefined, undefined)
    expect(properties.nodeData.status).toBe(200)
    expect(properties.nodeData.headers.length).toBeGreaterThan(0)

    // 应用目录下 status 属于未知字段，会被过滤并告警
    const { warnings } = sanitizeNodeDataPatch(applicationCatalog, 'response-node', { status: 200 })
    expect(warnings.some((w) => w.includes('status'))).toBe(true)
  })

  it('处理器开始节点条目：仅可修改不可添加，入参 patch 走白名单', () => {
    const entry = processorCatalog['start-node']
    expect(entry).toBeDefined()
    expect(entry.addable).toBe(false)
    // 应用画布目录没有 start-node（应用开始节点不可修改，无需条目）
    expect(applicationCatalog['start-node']).toBeUndefined()

    // HTTP 入参字段在白名单内，未知字段被过滤并告警
    const { patch, warnings } = sanitizeNodeDataPatch(processorCatalog, 'start-node', {
      method: 'GET',
      path: '/users',
      parameters: [],
      protocol: 'HTTP' // protocol 由系统管理，不在白名单
    })
    expect(patch).toEqual({ method: 'GET', path: '/users', parameters: [] })
    expect(warnings.some((w) => w.includes('protocol'))).toBe(true)
  })

  it('处理器基础场景（查询接口）：AI 产物通过节点真实校验器', () => {
    // database-search：填连接池 + SQL 模板
    const db = buildNodeProperties(processorCatalog, 'database-search-node', '查询所有用户', {
      poolId: 'pool-1',
      template: 'SELECT * FROM users'
    })
    expect(db.warnings).toEqual([])
    expect(validateDatabaseSearch(db.properties.nodeData).valid).toBe(true)

    // response：引用查询结果输出 JSON（默认 status/headers 即可过校验）
    const resp = buildNodeProperties(processorCatalog, 'response-node', '返回用户列表', {
      jsonFields: [
        { field: 'users', location: 'reference', reference: ['db-node-id', 'result'], required: true }
      ]
    })
    expect(resp.warnings).toEqual([])
    expect(validateProcessorResponse(resp.properties.nodeData).valid).toBe(true)
  })

  it('agent 标准工具映射的节点类型在所有画布目录中且定义提取成功', () => {
    expect(Object.keys(AGENT_TOOL_DEFINITIONS).length).toBeGreaterThanOrEqual(12)
    for (const profile of getAllProfiles()) {
      for (const nodeType of Object.values(AGENT_TOOL_NODE_MAP)) {
        expect(profile.catalog[nodeType], `${profile.workflowType}/${nodeType}`).toBeDefined()
      }
    }
  })
})
