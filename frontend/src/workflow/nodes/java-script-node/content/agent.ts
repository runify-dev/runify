import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { javaScriptNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * java-script-node（JavaScript 执行）的 AI 生成配置（命令式，手写）。
 * 「创作型」节点：代码是要现写的产物，故不走 update 一把塞，而是 read_code/write_code/diff_code 迭代着写。
 * nodeData：mode + 三元组 code(codeLocation/codeReference/code) + functionName + allowIO + parameters。
 */

const BASE = javaScriptNode.properties.nodeData
const outputs: OutputDef[] = [
  { value: 'result', label: '结果', type: 'any', description: '代码最后一个表达式的值' }
]
const FIELD_LIST = deriveFieldList(outputs)

/**
 * 入参签名（= update 的 parameters）：本节点可配的字段。代码不在其中——它是要现写的产物，
 * 走 read_code/write_code/diff_code 迭代，由本节点 agent 结合整体目标+画布自行写。
 */
const input: NodeInputSchema = {
  type: 'object',
  properties: {
    mode: { type: 'string', enum: ['script', 'function'], description: '推荐 script' },
    parameters: {
      type: 'array',
      description:
        '注入为顶层变量的上游数据，元素 { field 变量名, location:"reference"|"customize", value }（reference 时 value=[节点ID,字段]）'
    },
    allowIO: { type: 'boolean', description: '是否允许 IO/进程访问（默认 false）' },
    functionName: { type: 'string', description: 'mode="function" 时必填' }
  }
}

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  /** 把当前 nodeData 落库并刷新 field_list（结果字段固定 result） */
  const persist = (next: Record<string, any>) => {
    node.properties.nodeData = next
    node.properties.field_list = cloneDeep(FIELD_LIST)
  }
  /** 读当前已写的字面代码（customize 时的 code 串） */
  const readCode = (): string => {
    const data = node.properties.nodeData ?? {}
    return typeof data.code === 'string' ? data.code : ''
  }
  /** 写入字面代码：codeLocation=customize + code 串 */
  const writeCode = (code: string) => {
    const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
    next.codeLocation = 'customize'
    next.code = code
    delete next.codeReference
    persist(next)
  }
  return useAgentConfig(
    {
      description: '执行一段 JavaScript 代码做数据加工/计算，返回结果供下游引用',
      prompt:
        'java-script-node（JavaScript 执行）：只配本节点（已绑定目标节点，各工具无需传 nodeId）。这是「创作型」节点——代码由你结合整体目标+画布现写。\n' +
        '步骤：\n' +
        '1) 先 update 配 mode(推荐 script)/parameters/allowIO：parameters 把上游数据注入为顶层变量，元素 { field 变量名, location:"reference"|"customize", value }；reference 时 value 填真实 [节点ID,字段]（先 get_node_field_options 拿真值，别臆造节点 id）。\n' +
        '2) 按 spec 写代码：read_code 看现状 → write_code 写整段（小改用 diff_code）。代码里直接用上一步 parameters 声明的变量名；script 模式下最后一个表达式的值即结果。\n' +
        '   例：parameters=[{field:"hits",location:"reference",value:["知识检索节点ID","hits"]}]，代码 hits.map(h=>h.content).join("\\n")。\n' +
        '3) validate 收尾。输出字段：result（代码最后一个表达式的值）。',
      tools: [
        {
          name: 'update',
          description: '配置当前 JavaScript 节点的模式/注入参数/IO（不含代码；代码用 write_code/diff_code）。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            for (const key of ['mode', 'functionName', 'allowIO', 'parameters']) {
              if (args[key] !== undefined) next[key] = cloneDeep(args[key])
            }
            persist(next)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list: FIELD_LIST, errors: _vr.errors }
            return { status: 'stop', data: { field_list: FIELD_LIST } }
          }
        },
        {
          name: 'read_code',
          description: '读取本节点当前已写的 JavaScript 代码（返回 { code }）。',
          parameters: { type: 'object', properties: {} },
          apply: () => ({ code: readCode() })
        },
        {
          name: 'write_code',
          description: '把本节点的 JavaScript 代码整体写成 code（覆盖旧代码）。',
          parameters: {
            type: 'object',
            properties: { code: { type: 'string', description: '完整的 JavaScript 代码' } },
            required: ['code']
          },
          apply: (args) => {
            if (typeof args.code !== 'string') return { error: 'code 必须是字符串' }
            writeCode(args.code)
            return { ok: true, length: args.code.length }
          }
        },
        {
          name: 'diff_code',
          description: '对现有代码做局部替换：把唯一出现的 oldText 换成 newText（大改用 write_code）。',
          parameters: {
            type: 'object',
            properties: {
              oldText: { type: 'string', description: '要被替换的原片段（须在当前代码中唯一出现）' },
              newText: { type: 'string', description: '替换后的新片段' }
            },
            required: ['oldText', 'newText']
          },
          apply: (args) => {
            const cur = readCode()
            const { oldText, newText } = args
            if (typeof oldText !== 'string' || !oldText) return { error: 'oldText 不能为空' }
            if (!cur.includes(oldText)) return { error: 'oldText 未在当前代码中找到，请先 read_code 对照' }
            if (cur.indexOf(oldText) !== cur.lastIndexOf(oldText))
              return { error: 'oldText 命中多处，请给更长的唯一片段' }
            writeCode(cur.replace(oldText, typeof newText === 'string' ? newText : ''))
            return { ok: true }
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
  type: 'java-script-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
