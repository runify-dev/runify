import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { runToolNode } from '@/workflow/common/data'

/**
 * run-tool-node（调用工具）的 AI 生成配置（命令式，手写；无旧 catalog、无 validator）。
 * nodeData：toolId（工具资源 id）+ inputs（入参绑定）+ config（工具自定义配置）。
 * 入参结构依赖工具资源详情，AI 一般需用户在面板细配——只透传并轻校验 toolId。
 */

const BASE = runToolNode.properties.nodeData
const outputs: OutputDef[] = [
  { value: 'result', label: '执行结果', type: 'any' }
]
const FIELD_LIST = deriveFieldList(outputs)

const input: NodeInputSchema = {
            type: 'object',
            properties: {
              toolId: { type: 'string', description: '工具资源 id' },
              inputs: { type: 'array', description: '入参绑定数组（依赖该工具的入参定义）' },
              config: { type: 'object', description: '工具自定义配置对象' }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description:
        '调用一个已封装的工具资源，返回执行结果（入参依赖具体工具，通常需用户在面板细配）',
      prompt:
        'run-tool-node（调用工具）：用 update 配置。\n' +
        '- toolId：要调用的工具资源 id。inputs：入参绑定数组（依赖该工具的入参定义）。config：工具自定义配置对象。\n' +
        '- 入参结构因工具而异，AI 若不掌握该工具的入参定义，配好 toolId 后在 finish 里提醒用户在面板完善入参。\n' +
        '- 输出字段：result 执行结果。',
      tools: [
        {
          name: 'update',
          description: '配置当前调用工具节点：工具 id、入参、配置。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            for (const key of ['toolId', 'inputs', 'config']) {
              if (args[key] !== undefined) next[key] = cloneDeep(args[key])
            }
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            // 落库后自动校验（内联轻校验：toolId 必填）：通过即 stop 收尾
            const data = node.properties?.nodeData ?? {}
            if (!String(data.toolId ?? '').trim())
              return { field_list: FIELD_LIST, errors: { toolId: '请指定工具' } }
            return { status: 'stop', data: { field_list: FIELD_LIST } }
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
  type: 'run-tool-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
