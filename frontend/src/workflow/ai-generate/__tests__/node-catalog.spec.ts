import { describe, it, expect } from 'vitest'
import {
  nodeCatalog,
  catalogTypes,
  renderDoc,
  buildNodeProperties,
  sanitizeNodeDataPatch
} from '../node-catalog'
import { AGENT_TOOL_DEFINITIONS, AGENT_TOOL_NODE_MAP } from '../agent-tools'

describe('node-catalog 结构完整性', () => {
  it('每个节点都能渲染文档且包含输入/输出段', () => {
    for (const type of catalogTypes) {
      const doc = renderDoc(nodeCatalog[type])
      expect(doc, type).toContain('输入（nodeData 字段）')
      expect(doc, type).toContain('输出字段')
    }
  })

  it('每个节点都能生成默认 properties（含 field_list 与 nodeData）', () => {
    for (const type of catalogTypes) {
      const { properties, warnings } = buildNodeProperties(type, undefined, undefined)
      expect(warnings, type).toEqual([])
      expect(properties.nodeData, type).toBeTypeOf('object')
      expect(Array.isArray(properties.field_list), type).toBe(true)
    }
  })

  it('静态输出节点的 field_list 与 outputs（非隐藏）一致', () => {
    for (const type of catalogTypes) {
      const entry = nodeCatalog[type]
      if (entry.applyFieldList) continue // 动态输出节点跳过
      const { properties } = buildNodeProperties(type, undefined, undefined)
      const expected = entry.outputs.filter((o) => !o.hidden).map((o) => o.value)
      expect(properties.field_list.map((f: any) => f.value), type).toEqual(expected)
    }
  })

  it('未知字段被过滤并产生警告', () => {
    const { patch, warnings } = sanitizeNodeDataPatch('knowledge-search-node', {
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
    const { warnings } = sanitizeNodeDataPatch('loop-node', { loopType: 'while' })
    expect(warnings.some((w) => w.includes('loopType'))).toBe(true)
  })

  it('loop 默认初始化 loop-start-node，loopVariables 自动推断 dataType', () => {
    const { properties } = buildNodeProperties('loop-node', undefined, {
      loopVariables: [{ name: 'context', defaultValue: '[]' }]
    })
    expect(properties.nodeData.children.nodes[0].type).toBe('loop-start-node')
    expect(properties.nodeData.loopVariables[0].dataType).toBe('array')
    expect(properties.field_list.map((f: any) => f.value)).toContain('context')
  })

  it('judge 自动补分支/条件 id 并保证 else 分支', () => {
    const { properties } = buildNodeProperties('judge-node', undefined, {
      branches: [{ type: 'if', conditions: [{ variable: ['a', 'b'], compare: 'eq', value: '1' }] }]
    })
    const branches = properties.nodeData.branches
    expect(branches.every((b: any) => !!b.id)).toBe(true)
    expect(branches.some((b: any) => b.type === 'else')).toBe(true)
  })

  it('ai-chat 标准工具名自动替换为完整标准定义', () => {
    const { properties } = buildNodeProperties('ai-chat-node', undefined, {
      modelId: 'm1',
      tools: { location: 'customize', reference: [], tools: [{ type: 'function', function: { name: 'grep' } }] }
    })
    const tool = properties.nodeData.tools.tools[0]
    expect(tool.function.parameters).toBeDefined()
    expect(tool.function.parameters.properties.pattern).toBeDefined()
  })

  it('agent 标准工具映射的节点类型都在目录中且定义提取成功', () => {
    expect(Object.keys(AGENT_TOOL_DEFINITIONS).length).toBeGreaterThanOrEqual(12)
    for (const nodeType of Object.values(AGENT_TOOL_NODE_MAP)) {
      expect(nodeCatalog[nodeType], nodeType).toBeDefined()
    }
  })
})
