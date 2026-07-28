import type { NodeInitContext } from '@/workflow/common/type'

const labelOf = (f: any) => {
  if (f?.label && typeof f.label === 'object') return f.label.value || f.field
  return f?.label || f?.field
}
const toField = (f: any) => ({ label: labelOf(f), value: f.field })

// 内置：调用方上下文（谁调用谁知道），运行时由 run-tool 注入。
// 只带调用方类型，不带 应用/会话/用户 id（调用方私有信息，工具无需知道）。
export const CALLER_GROUP = {
  label: '调用方',
  value: 'caller',
  children: [{ label: '类型(chat/processor)', value: 'type' }]
}

/**
 * 由工具设置(details)构建开始节点的画布字段：
 * input(+config 分组)+caller → field_list（下游引用 start-node.<字段> / start-node.config.<字段>）
 * output → outputFieldList（变量赋值写「参数输出」output.<字段>，即工具返回值）
 */
export function buildToolStartFields(details: any) {
  const d = details || {}
  const inputFields = (d.inputSchema || []).filter((f: any) => f.field).map(toField)
  const configFields = (d.configSchema || []).filter((f: any) => f.field).map(toField)
  const outputFields = (d.outputSchema || []).filter((f: any) => f.field).map(toField)

  const fieldList: any[] = [...inputFields]
  if (configFields.length) {
    fieldList.push({ label: '配置', value: 'config', children: configFields })
  }
  fieldList.push(CALLER_GROUP)
  return { fieldList, outputFieldList: outputFields }
}

/**
 * 工具工作流开始节点 init。注意：起始节点是画布虚拟渲染的组件，mount 时机不可靠，
 * 真正的权威同步在宿主(Details.vue)按 details 变化直接写模型；此处仅作节点自身挂载时的兜底。
 * globalFieldList 交回自由暂存 global，不再等于 outputSchema。
 */
export function init(ctx: NodeInitContext) {
  const { fieldList, outputFieldList } = buildToolStartFields((ctx as any).details)
  ctx.model.properties.field_list = fieldList
  ctx.model.properties.outputFieldList = outputFieldList
  if (!ctx.model.properties.globalFieldList) {
    ctx.model.properties.globalFieldList = []
  }
}
