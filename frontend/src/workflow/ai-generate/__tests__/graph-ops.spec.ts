import { describe, it, expect } from 'vitest'
import { mergeJudgeBranches, lintWorkflowGraph } from '../graph-ops'

const anchor = (id: string, branch: string, status = 'success') => `${id}_right_${branch}_${status}`

describe('mergeJudgeBranches：judge 分支按 id 合并', () => {
  const existing = [
    { id: 'b-if', type: 'if', logic: 'and', conditions: [] },
    { id: 'b-else', type: 'else', logic: 'and', conditions: [] }
  ]

  it('incoming 带原 id 时全部沿用，不产生移除', () => {
    const { branches, removedIds } = mergeJudgeBranches(existing, [
      { id: 'b-if', type: 'if', conditions: [{ variable: ['x', 'y'], compare: 'eq', value: '1' }] },
      { id: 'b-else', type: 'else' }
    ])
    expect(branches.map((b: any) => b.id)).toEqual(['b-if', 'b-else'])
    expect(removedIds).toEqual([])
  })

  it('incoming 不带 id 时按类型+顺序认领旧 id（连线不被打断）', () => {
    const { branches, removedIds } = mergeJudgeBranches(existing, [
      { type: 'if', conditions: [] },
      { type: 'else' }
    ])
    expect(branches.map((b: any) => b.id)).toEqual(['b-if', 'b-else'])
    expect(removedIds).toEqual([])
  })

  it('新增分支保持无 id（由 normalize 生成），同类旧分支不被抢占', () => {
    const { branches, removedIds } = mergeJudgeBranches(existing, [
      { id: 'b-if', type: 'if', conditions: [] },
      { type: 'elseif', conditions: [] },
      { type: 'else' }
    ])
    expect(branches[0].id).toBe('b-if')
    expect(branches[1].id).toBeUndefined()
    expect(branches[2].id).toBe('b-else')
    expect(removedIds).toEqual([])
  })

  it('删掉的分支进入 removedIds', () => {
    const three = [...existing, { id: 'b-elseif', type: 'elseif', conditions: [] }]
    const { removedIds } = mergeJudgeBranches(three, [
      { id: 'b-if', type: 'if', conditions: [] },
      { id: 'b-else', type: 'else' }
    ])
    expect(removedIds).toEqual(['b-elseif'])
  })
})

describe('lintWorkflowGraph：结构检查', () => {
  const node = (id: string, type: string, name: string, nodeData: any = {}) => ({
    id,
    type,
    properties: { name, nodeData }
  })
  const edge = (source: string, target: string, branch = 'main') => ({
    sourceNodeId: source,
    targetNodeId: target,
    sourceAnchorId: anchor(source, branch)
  })

  it('健康图无告警', () => {
    const result = lintWorkflowGraph({
      nodes: [node('start-node', 'start-node', '开始'), node('a', 'ai-chat-node', 'AI 对话')],
      edges: [edge('start-node', 'a')]
    })
    expect(result.errors).toEqual([])
    expect(result.warnings).toEqual([])
  })

  it('同名同类型重复节点 → warning（用户案例：两个上下文写入）', () => {
    const result = lintWorkflowGraph({
      nodes: [
        node('start-node', 'start-node', '开始'),
        node('cs1', 'context-save-node', '上下文写入'),
        node('cs2', 'context-save-node', '上下文写入')
      ],
      edges: [edge('start-node', 'cs1'), edge('start-node', 'cs2')]
    })
    expect(result.warnings.length).toBe(1)
    expect(result.warnings[0]).toContain('上下文写入')
    expect(result.warnings[0]).toContain('cs1')
    expect(result.warnings[0]).toContain('cs2')
  })

  it('孤立节点 → error；入口节点豁免', () => {
    const result = lintWorkflowGraph({
      nodes: [node('start-node', 'start-node', '开始'), node('a', 'ai-chat-node', 'AI 对话')],
      edges: []
    })
    expect(result.errors.length).toBe(1)
    expect(result.errors[0]).toContain('孤立节点')
  })

  it('悬空边（端点不存在）→ error', () => {
    const result = lintWorkflowGraph({
      nodes: [node('start-node', 'start-node', '开始')],
      edges: [edge('start-node', 'ghost')]
    })
    expect(result.errors.some((e) => e.includes('悬空连线'))).toBe(true)
  })

  it('judge 分支 id 失效的出边 → error', () => {
    const result = lintWorkflowGraph({
      nodes: [
        node('start-node', 'start-node', '开始'),
        node('j', 'judge-node', '判断', { branches: [{ id: 'b-new', type: 'if' }] }),
        node('a', 'ai-chat-node', 'AI 对话')
      ],
      edges: [edge('start-node', 'j'), edge('j', 'a', 'b-old')]
    })
    expect(result.errors.some((e) => e.includes('b-old'))).toBe(true)
  })

  it('递归检查循环子画布，入口为 loop-start-node', () => {
    const result = lintWorkflowGraph({
      nodes: [
        node('start-node', 'start-node', '开始'),
        node('loop1', 'loop-node', '循环', {
          children: {
            nodes: [
              node('loop-start-node', 'loop-start-node', '循环开始'),
              node('inner', 'ai-chat-node', '内部对话')
            ],
            edges: []
          }
        })
      ],
      edges: [edge('start-node', 'loop1')]
    })
    expect(result.errors.length).toBe(1)
    expect(result.errors[0]).toContain('循环')
    expect(result.errors[0]).toContain('内部对话')
  })
})
