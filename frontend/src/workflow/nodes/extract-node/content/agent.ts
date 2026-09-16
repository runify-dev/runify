import { cloneDeep } from 'lodash'
import { resolveNode, useAgentConfig, deriveFieldList } from '@/workflow/ai-generate/common'
import { getNodeFieldOptionsTool, getTemplateVariablesTool } from '@/workflow/ai-generate/tools'
import type { AgentBindings, OutputDef, NodeAgentModule, NodeInputSchema } from '@/workflow/ai-generate/type'
import { validate as validateNode } from './validator'

/**
 * extract-node（参数提取）的 AI 生成配置（命令式，手写）。
 * nodeData：sourceReference（源变量 [节点ID,字段]）+ rules（JSONPath 提取规则）。
 * 输出字段由 rules 动态生成：每条规则一个（value=rule.name，label=rule.description||name）。
 */

/** 出参签名（动态）：每条 rule 一个输出字段，值类型未知记 any */
const outputs = (nodeData: any): OutputDef[] =>
  (nodeData?.rules ?? [])
    .filter((r: any) => r.name && String(r.name).trim())
    .map((r: any) => ({ value: r.name, label: r.description || r.name, type: 'any' as const }))

/** 由 rules 动态刷新 field_list（出参单一来源） */
function applyFieldList(node: any): { label: string; value: string }[] {
  const fieldList = deriveFieldList(outputs(node.properties?.nodeData))
  node.properties.field_list = fieldList
  return fieldList
}

/** 入参签名（= update 的 parameters，单一来源） */
const input: NodeInputSchema = {
  type: 'object',
  properties: {
    sourceReference: {
      type: 'array',
      items: { type: 'string' },
      description: '源变量 [节点ID,字段]'
    },
    rules: {
      type: 'array',
      description: '提取规则数组。元素 { name, description, path(JSONPath) }。'
    }
  }
}

function useAgent(bindings: AgentBindings) {
  const node = resolveNode(bindings.lf, bindings.position?.[bindings.position.length - 1] ?? '')
  return useAgentConfig(
    {
      description: '用 JSONPath 从上游变量（JSON/对象）中提取字段，产出可被下游引用的新变量',
      prompt:
        'extract-node（参数提取）：用 update 配置。\n' +
        '- sourceReference 必填：源变量 [节点ID,字段]（指向 JSON/对象）。\n' +
        '- rules 必填：提取规则数组，元素 { name 字段名(成为输出字段), description 说明, path JSONPath 如 "$.functionName" 或 "$[0].id" }。\n' +
        '- 输出字段由 rules 动态生成：每条规则一个输出字段（value=name）。',
      tools: [
        {
          name: 'update',
          description: '配置当前参数提取节点：源变量、提取规则。输出字段随 rules 动态生成。',
          parameters: input,
          apply: (args) => {
            const next = { ...(node.properties.nodeData ?? {}) }
            if (args.sourceReference !== undefined)
              next.sourceReference = cloneDeep(args.sourceReference)
            if (args.rules !== undefined) next.rules = cloneDeep(args.rules)
            node.properties.nodeData = next
            const field_list = applyFieldList(node)
            const _vr = validateNode(node.properties?.nodeData ?? {})
            if (!_vr.valid) return { field_list, errors: _vr.errors }
            return { status: 'stop', data: { field_list } }
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
  type: 'extract-node',
  useAgent,
  input,
  outputs
} satisfies NodeAgentModule
