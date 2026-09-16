import { reactive } from 'vue'
import { cloneDeep } from 'lodash'
import { randomId, generateAnchor } from '@/utils/common'
import { resolveNode, useAgentConfig } from '@/workflow/ai-generate/common'
import { getCanvasDetailTool, getNodeFieldOptionsTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { conditionItemSchema } from '../type'
import { validate as validateNode } from './validator'

/**
 * judge-node（条件判断）的 AI 生成配置（命令式，手写）。
 * 分支结构固定：恰 1 个 if（首）+ 0..N 个 elseif + 1 个 else（尾）。每个分支是一个独立出口锚点，
 * 锚点 id 由分支 id 派生（generateAnchor(nodeId,'right',branchId,'success')，见 judge-node/index.ts）。
 * 故分支不做整体替换（会churn id、打断已连的边），而是逐个增/改/删——与画布表单
 * （content/index.vue 只增删 elseif）语义一致，delete 时顺带清理该分支的悬空出边。
 */

/**
 * 按分支数/条件数算 judge 节点高度（与 index.ts 的锚点几何同一套常量）。
 * judge 的出口锚点位置由 getDefaultAnchor 依 height 布局，故改分支后必须 setHeight 触发锚点重算/重渲染，
 * 否则画布上节点不变、分支锚点不出现（镜像表单 submit 里改 model.height 的效果，但这里不依赖 DOM 测量）。
 */
const CONDITION_HEIGHT = 28
const CONDITION_LOGIC_HEIGHT = 10
const ELSE_HEIGHT = 28
const BRANCH_GAP = 8
const NODE_HEADER_HEIGHT = 50
const CONTENT_PADDING = 8

function branchHeight(branch: any): number {
  if (branch.type === 'else') return ELSE_HEIGHT
  const n =
    Array.isArray(branch.conditions) && branch.conditions.length > 0 ? branch.conditions.length : 1
  return n * CONDITION_HEIGHT + Math.max(n - 1, 0) * CONDITION_LOGIC_HEIGHT
}

function judgeNodeHeight(branches: any[]): number {
  const total =
    branches.reduce((sum, b) => sum + branchHeight(b), 0) +
    Math.max(branches.length - 1, 0) * BRANCH_GAP
  return NODE_HEADER_HEIGHT + CONTENT_PADDING + total + CONTENT_PADDING
}

/** 补齐单个分支的 id / logic / conditions（含各 condition 的 id / variable / value 缺省） */
function normalizeBranch(branch: any): any {
  if (!branch.id) branch.id = randomId()
  if (branch.type !== 'else') {
    if (!branch.logic) branch.logic = 'and'
    if (!Array.isArray(branch.conditions)) branch.conditions = []
    for (const condition of branch.conditions) {
      if (!condition.id) condition.id = randomId()
      if (!Array.isArray(condition.variable)) condition.variable = []
      if (!condition.location) condition.location = 'customize'
      if (!Array.isArray(condition.referenceValue)) condition.referenceValue = []
      if (condition.value === undefined) condition.value = ''
    }
  }
  return branch
}

const outputs: OutputDef[] = []
const input: NodeInputSchema = { type: 'object', properties: {} }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')

  /** 读当前分支（cloneDeep，改完再整体写回） */
  const readBranches = (): any[] =>
    Array.isArray(node.properties.nodeData?.branches)
      ? cloneDeep(node.properties.nodeData.branches)
      : []

  /** 写回分支：保证末尾有 else；nodeData 经 Vue 响应式代理写入以通知画布节点组件重渲染，再按分支 setHeight 触发锚点重算 */
  const writeBranches = (branches: any[]) => {
    if (!branches.some((b) => b.type === 'else')) {
      branches.push({ id: randomId(), type: 'else' })
    }
    // 画布上的 judge 是 Vue 组件，其 model 为 reactive(node)（见 common/teleport.ts）；直接改原始 mobx 模型
    // Vue 代理收不到 set 通知、节点不重渲染。故 nodeData 经 reactive(node) 写入（Vue 按 target 缓存同一代理），
    // 触发组件里 branches 计算属性更新、分支列表重渲染。
    reactive(node).properties.nodeData = { ...(node.properties.nodeData ?? {}), branches }
    // 高度/锚点走原始模型：setHeight 改 height（mobx observable）触发 getDefaultAnchor 重算分支出口锚点，并刷新已连边路径
    node.setHeight?.(judgeNodeHeight(branches))
    node.refreshDegrees?.()
  }

  /** 删除某分支出口锚点上已连的下游边（分支删除后其锚点消失，边会悬空） */
  const deleteBranchEdges = (branchId: string): number => {
    const g = node.graphModel
    const anchorId = generateAnchor(node.id, 'right', branchId, 'success')
    const doomed = g.edges.filter(
      (e: any) => e.sourceNodeId === node.id && e.sourceAnchorId === anchorId
    )
    doomed.forEach((e: any) => g.deleteEdgeById(e.id))
    return doomed.length
  }

  return useAgentConfig(
    {
      description:
        '按条件分支执行，每个分支是独立出口锚点；用 get_branches/add_branch/update_branch/delete_branch 逐个管理分支（连线时父 agent 用 get_node_anchors 取分支锚点作 add_edge 的 source_anchor_id）',
      prompt:
        'judge-node（条件判断）：分支决定下游走向，每个分支 = 一个独立出口锚点，其 id 即锚点，父 agent 靠它把各分支接到不同下游。\n' +
        '- 固定结构：恰 1 个 if（第一个）+ 0..N 个 elseif + 1 个 else（最后）。if 与 else 固定存在、不可增删；elseif 用 add_branch 增、delete_branch 删。\n' +
        '- 先 get_branches 看现有分支（默认只有 1 个 if + 1 个 else）。\n' +
        '★ 先想清楚这个 judge 要分几路、每路判什么——这决定你要建几个分支，是本节点最关键的一步：\n' +
        '  · 普通判断：按整体目标把 if（及需要的 elseif）的条件配好即可。\n' +
        '  · 【按工具名分流】（agent 循环里最常见：上游 ai-chat 声明了多个工具，模型每轮调其一，本 judge 要把不同工具分派到不同下游）——必须「一工具一分支」，不能只配一条：先 get_canvas_detail 读上游 ai-chat 的 declaredTools（真实函数名清单），再为其中每一个工具建一条分支（第一个工具用 update_branch 配到默认 if 上，其余每个工具各 add_branch 一条 elseif），条件均为 functionName eq <该工具名>；else 留作兜底。\n' +
        '    只配一条「非空」就收工是错的——那样所有工具都挤进同一路、分流失败（你截图里的直链就是这么来的）。value 只能取 declaredTools 里的真实名，勿臆造（createFile/generatePPT 之类会让分支永远落空）。\n' +
        '- add_branch(logic, conditions) 加一个 elseif（自动插在 else 前），返回其稳定 id（= 出口锚点，供父 agent 连线）；后续编辑不改这个 id、已连的边不断。\n' +
        '- update_branch(branchId, logic?, conditions?) 原地改某分支（if 或 elseif）的条件，id 不变；delete_branch(branchId) 删一个 elseif（自动清理其下游边）。\n' +
        '- 条件元素 { variable, compare, location, referenceValue, value }，凡引用上游输出的字段都先调 get_node_field_options 拿真实 [节点ID, 字段]，勿臆造：\n' +
        '  · variable（左值，必填）：对上游输出的引用，填数组 [节点ID, 字段]；引用「当前项」用 get_canvas_detail 的 youAreHere 给的本层真实循环 id。\n' +
        '  · location（右值来源）："customize" 用字面值 value；"reference" 用引用 referenceValue（和左值同构 [节点ID, 字段]）。\n' +
        '  · value（location=customize 时）：右值字面量（直接写字符串）；is_null/is_not_null/is_true/is_not_true 不需要右值。\n' +
        '  · compare 可选：eq/not_eq/ge/gt/le/lt/contain/not_contain/is_null/is_not_null/len_eq/len_ge/len_gt/len_le/len_lt/is_true/is_not_true/start_with/end_with/regex/wildcard。\n' +
        '- 配好后 validate 校验、再 get_branches 复核。\n' +
        '- 收尾时在结论里逐条列出你建了哪些分支：每条给出「分支类型（if/elseif/else）+ 判断条件」，让父 agent 知道有哪几个出口锚点、各自代表哪一路，好把对应下游接上去。',
      tools: [
        {
          name: 'get_branches',
          description:
            '查看本节点当前所有分支（含默认 if / else 及其 id、类型、逻辑、条件）。增改删分支前先调用它拿到分支 id。',
          parameters: { type: 'object', properties: {} },
          apply: () => ({
            branches: readBranches().map((b: any) => ({
              id: b.id,
              type: b.type,
              logic: b.logic,
              conditions: b.conditions
            }))
          })
        },
        {
          name: 'add_branch',
          description:
            '新增一个 elseif 分支（自动插在 else 前），返回其稳定 id（= 出口锚点，供连线用）。if 与 else 固定存在，勿用本工具加。',
          parameters: {
            type: 'object',
            properties: {
              logic: {
                type: 'string',
                enum: ['and', 'or'],
                description: '本分支多条件间的逻辑关系，缺省 and'
              },
              conditions: {
                type: 'array',
                description: '本分支的条件列表（一次 add_branch 只新增这一个 elseif 分支）',
                items: conditionItemSchema
              }
            }
          },
          apply: (args) => {
            const branches = readBranches()
            const branch = normalizeBranch({
              type: 'elseif',
              logic: args.logic,
              conditions: args.conditions
            })
            const elseIndex = branches.findIndex((b) => b.type === 'else')
            if (elseIndex === -1) branches.push(branch)
            else branches.splice(elseIndex, 0, branch)
            writeBranches(branches)
            return { id: branch.id, type: branch.type }
          }
        },
        {
          name: 'update_branch',
          description:
            '原地修改某分支（if 或 elseif）的逻辑 / 条件，分支 id 不变、已连的边不受影响。只传要改的字段。',
          parameters: {
            type: 'object',
            properties: {
              branchId: { type: 'string', description: '要修改的分支 id（get_branches 获取）' },
              logic: {
                type: 'string',
                enum: ['and', 'or'],
                description: '本分支多条件间的逻辑关系'
              },
              conditions: {
                type: 'array',
                description: '整体覆盖该分支的条件列表',
                items: conditionItemSchema
              }
            },
            required: ['branchId']
          },
          apply: (args) => {
            const branches = readBranches()
            const branch = branches.find((b) => b.id === args.branchId)
            if (!branch) return { error: `分支不存在: ${args.branchId}` }
            if (branch.type === 'else') return { error: 'else 分支无条件可配' }
            if (args.logic !== undefined) branch.logic = args.logic
            if (args.conditions !== undefined) branch.conditions = args.conditions
            normalizeBranch(branch)
            writeBranches(branches)
            return { id: branch.id, type: branch.type }
          }
        },
        {
          name: 'delete_branch',
          description:
            '删除一个 elseif 分支，并自动清理该分支出口已连的下游边。if 与 else 固定保留，不可删除。',
          parameters: {
            type: 'object',
            properties: {
              branchId: { type: 'string', description: '要删除的 elseif 分支 id（get_branches 获取）' }
            },
            required: ['branchId']
          },
          apply: (args) => {
            const branches = readBranches()
            const branch = branches.find((b) => b.id === args.branchId)
            if (!branch) return { error: `分支不存在: ${args.branchId}` }
            if (branch.type !== 'elseif') {
              return { error: '只能删除 elseif 分支（if 与 else 固定保留）' }
            }
            const removedEdges = deleteBranchEdges(args.branchId)
            writeBranches(branches.filter((b) => b.id !== args.branchId))
            return { deleted: args.branchId, removedEdges }
          }
        },
        {
          name: 'validate',
          // judge 无单一 update 落库（分支靠增删改工具多步配）：全部分支配好后调 validate 收尾——
          // 通过即返回 stop 结束本节点配置 agent，不通过返回 errors 继续修。
          description:
            '全部分支配好后调用它校验并收尾：校验当前条件判断节点配置是否完整合法。通过即代表配置完成、你的任务到此结束；不通过则返回 errors 继续修。',
          parameters: { type: 'object', properties: {} },
          apply: (_args) => {
            const result = validateNode(node.properties?.nodeData ?? {})
            if (!result.valid) return { valid: false, errors: result.errors }
            return { status: 'stop', data: { valid: true } }
          }
        },
        getCanvasDetailTool(bindings),
        getNodeFieldOptionsTool(bindings)
      ]
    },
    bindings
  )
}

export default {
  type: 'judge-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
