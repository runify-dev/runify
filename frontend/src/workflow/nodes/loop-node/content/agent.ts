import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { loopNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * loop-node（循环）的 AI 生成配置（命令式，手写）。
 * 只配「循环本身」的字段：update/add_loop_variable 配类型/遍历数组/次数/作用域变量，validate 收尾。
 * 循环体不由本节点搭——父 agent 用 add_node/configure_node 带 lf_id=本循环 id 直接往循环体子画布里建、配
 * （单一 agent 跨画布操作，见 shared.ts 的 lf_id 机制；不再起绑定子画布的循环体构建子 agent）。
 * loopVariables 既是循环作用域变量也是本循环的输出字段。子画布(children) update 不碰。
 */

/**
 * 循环节点 agent 的 system prompt：只讲怎么配循环本身的字段。循环体由父 agent 经 lf_id 直接建，不在这里触发。
 */
const LOOP_PROMPT =
  'loop-node（循环）：你只负责配「循环本身」的字段（循环体不归你建——父 agent 会用 add_node/configure_node 带 lf_id=本循环 id 直接往循环体里建）。\n' +
  '1) 用 update 配循环字段：按整体目标与画布结构判断 loopType 三选一：infinite（无限，agent 主循环即此，只需这一个字段）/ foreach（遍历某数组，还需 loopVariable）/ count（固定次数，还需 loopCount）。\n' +
  '   foreach 的 loopVariable 是"要遍历的数组"：调 get_node_field_options 找到该数组的真实 ["节点id","字段"]（如上游 ai-chat 的 toolCalls），别拿类型名当 id。各字段确切格式见 update 参数说明，按需只传该传的。\n' +
  '   ★ 若 plan 的变量契约给本循环指定了循环变量（如 agent 记忆的 context/summary/facts），逐个用 add_loop_variable 声明（幂等，已存在跳过）——循环体里的 context-manage/ai-chat/variable-assign、循环之后的 context-save 要引用的循环变量都必须先声明,否则它们引用不到。\n' +
  '循环字段配好、循环变量按契约声明好，用 validate 校验通过后，用一段话总结即结束。'

const BASE = loopNode.properties.nodeData

/** 循环作用域变量 dataType → 出参类型 */
const DATATYPE_MAP: Record<string, OutputDef['type']> = {
  string: 'string',
  number: 'number',
  boolean: 'boolean',
  array: 'array',
  dict: 'object'
}

/** 出参签名（动态）：固定 item/index + loopVariables 定义的变量 */
const outputs = (nodeData: any): OutputDef[] => {
  const vars = Array.isArray(nodeData?.loopVariables) ? nodeData.loopVariables : []
  return [
    { value: 'item', label: '当前项', type: 'any', description: 'foreach 时为数组当前元素' },
    { value: 'index', label: '当前索引', type: 'number' },
    ...vars
      .filter((v: any) => v?.name)
      .map((v: any) => ({
        value: v.name,
        label: v.label || v.name,
        type: DATATYPE_MAP[v.dataType] ?? 'any'
      }))
  ]
}

/** field_list = 固定 item/index + loopVariables 定义的变量（由 outputs 派生） */
function buildFieldList(nodeData: any): { label: string; value: string }[] {
  return deriveFieldList(outputs(nodeData))
}

/** 入参签名（= update 的 parameters，单一来源；不含 children，循环体由父 agent 经 lf_id 直接搭建） */
const input: NodeInputSchema = {
  type: 'object',
  properties: {
    loopType: {
      type: 'string',
      enum: ['foreach', 'infinite', 'count'],
      description: '循环类型'
    },
    loopVariable: {
      type: 'array',
      description:
        '仅 foreach 用：要遍历的那个数组，恰好两个字符串 ["真实节点id","字段名"]（如 ["<ai-chat真实id>","toolCalls"]）。infinite/count 不要传。'
    },
    loopCount: { type: 'number', description: '仅 count 用：循环次数（正整数）。' },
    loopVariables: {
      type: 'array',
      description:
        '循环作用域累加变量（跨轮状态，同时也是本循环的输出字段）：若 plan 的变量契约为本循环指定了循环变量' +
        '（如 agent 记忆的 context(array,[])/summary(dict,{})/facts(array,[])，或任务所需累加器），在此按契约逐个声明。' +
        '循环体里的 context-manage/ai-chat/variable-assign、循环之后的 context-save 要引用的循环变量，必须先在这里声明，' +
        '否则它们 get_node_field_options 里查不到、引用不上。与 loopVariable(遍历数组)无关。' +
        '元素 { name, label, dataType("string"|"number"|"boolean"|"array"|"dict"), defaultValue(字符串) }'
    }
  }
}

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '循环执行子画布（数组遍历/无限/指定次数）；只配置循环本身，循环体由框架按蓝图搭建',
      prompt: LOOP_PROMPT,
      tools: [
        {
          name: 'update',
          description:
            '配置当前循环节点：循环类型、遍历数组/次数、作用域变量。只传要设置的字段（不含 children）。',
          parameters: input,
          apply: (args) => {
            // 兜底硬校验：foreach 的 loopVariable[0] 必须真实存在——同层兄弟节点（resolveNode 本画布）、
            // 或所在/外层循环 id（loopContext）、或 global；堵住「把节点类型名当 id」这类臆造
            // （如 ["ai-chat-node","toolCalls"]，正解应是真实 ai-chat 节点 id）。
            if (Array.isArray(args.loopVariable) && args.loopVariable.length) {
              const refId = args.loopVariable[0]
              const ok =
                refId === 'global' ||
                refId === bindings.loopContext?.selfLoopId ||
                refId === bindings.loopContext?.outerLoopId ||
                !!resolveNode(bindings.lf, refId)
              if (!ok) {
                return {
                  error:
                    `loopVariable 的节点 id "${refId}" 在当前画布上不存在（疑似把节点类型名当成了 id）。` +
                    '请先调 get_node_field_options 取真实节点 id 后重填。'
                }
              }
            }
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            for (const key of ['loopType', 'loopVariable', 'loopCount', 'loopVariables']) {
              if (args[key] !== undefined) next[key] = cloneDeep(args[key])
            }
            // 规范化 loopVariables（label/defaultValue 字符串化）
            if (Array.isArray(next.loopVariables)) {
              for (const v of next.loopVariables) {
                if (!v || typeof v !== 'object') continue
                if (!v.label) v.label = v.name
                if (v.defaultValue != null && typeof v.defaultValue !== 'string')
                  v.defaultValue = JSON.stringify(v.defaultValue)
              }
            }
            node.properties.nodeData = next
            const field_list = buildFieldList(next)
            node.properties.field_list = field_list
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list, errors: _vr.errors }
            return { status: 'stop', data: { field_list } }
          }
        },
        {
          name: 'add_loop_variable',
          description:
            '在本循环上声明一个循环作用域变量（跨轮累加器/记忆，如 agent 记忆的 context/summary/facts）。' +
            '幂等：已存在同名变量则跳过、不重复加。声明后，循环体内与循环之后的节点即可按 [本循环id, name] 引用它。' +
            '按 plan 的变量契约为本循环逐个声明；漏声明时也用它补。',
          parameters: {
            type: 'object',
            properties: {
              name: { type: 'string', description: '变量名（下游引用时的字段名）' },
              dataType: {
                type: 'string',
                enum: ['string', 'number', 'boolean', 'array', 'dict'],
                description: '数据类型'
              },
              label: { type: 'string', description: '显示名（缺省=name）' },
              defaultValue: {
                type: 'string',
                description: '默认值（array/dict 用 JSON 字符串，如 "[]" "{}"；缺省按类型给空值）'
              }
            },
            required: ['name', 'dataType']
          },
          apply: (args) => {
            if (!args.name || typeof args.name !== 'string') return { error: 'name 必填且为字符串' }
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            const list = Array.isArray(next.loopVariables) ? next.loopVariables : []
            // 幂等：同名已存在就跳过（防多个消费者/多轮重复声明造成同名打架）
            if (list.some((v: any) => v?.name === args.name)) {
              return { ok: true, existed: true, name: args.name, loopVariables: list.map((v: any) => v?.name) }
            }
            const DEFAULTS: Record<string, string> = {
              string: '',
              number: '0',
              boolean: 'false',
              array: '[]',
              dict: '{}'
            }
            list.push({
              name: args.name,
              label: args.label || args.name,
              dataType: args.dataType,
              defaultValue: args.defaultValue != null ? String(args.defaultValue) : (DEFAULTS[args.dataType] ?? '')
            })
            next.loopVariables = list
            node.properties.nodeData = next
            const field_list = buildFieldList(next)
            node.properties.field_list = field_list
            return { ok: true, name: args.name, loopVariables: list.map((v: any) => v?.name), field_list }
          }
        },
        getNodeFieldOptionsTool(bindings),
        getTemplateVariablesTool(bindings)
      ]
    },
    bindings
  )
}

export default {
  type: 'loop-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
