import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { staticFileNode } from '@/workflow/common/data'
import { validate as validateNode } from './validator'

/**
 * static-file-node（静态文件）的 AI 生成配置（命令式，手写；无旧 catalog）。
 * nodeData：fileId（用户上传的 ZIP）+ fileName。fileId 需用户上传，AI 无法生成——
 * 只透传已有值并在 validate 中提示缺失。
 */

const BASE = staticFileNode.properties.nodeData
const outputs: OutputDef[] = [
  { value: 'url', label: '访问地址', type: 'string' },
  { value: 'deployId', label: '部署 ID', type: 'string' }
]
const FIELD_LIST = deriveFieldList(outputs)

const input: NodeInputSchema = {
            type: 'object',
            properties: {
              fileId: { type: 'string', description: '静态资源 ZIP 文件 id（需用户上传）' },
              fileName: { type: 'string', description: '文件名' }
            }
          }

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '部署静态文件（ZIP），输出访问地址；ZIP 需用户上传，AI 无法代填 fileId',
      prompt:
        'static-file-node（静态文件）：用 update 配置。\n' +
        '- fileId：静态资源 ZIP 的文件 id（需用户在面板上传，AI 通常无法提供）；fileName 文件名。\n' +
        '- 若缺 fileId，validate 会提示、须在 finish 总结里提醒用户上传 ZIP。\n' +
        '- 输出字段：url 访问地址、deployId 部署 ID。',
      tools: [
        {
          name: 'update',
          description: '配置当前静态文件节点：文件 id、文件名。只传要设置的字段。',
          parameters: input,
          apply: (args) => {
            const next = { ...cloneDeep(BASE), ...(node.properties.nodeData ?? {}) }
            for (const key of ['fileId', 'fileName']) {
              if (args[key] !== undefined) next[key] = args[key]
            }
            node.properties.nodeData = next
            node.properties.field_list = cloneDeep(FIELD_LIST)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list: FIELD_LIST, errors: _vr.errors }
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
  type: 'static-file-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
