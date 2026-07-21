import type { NodeInitContext } from '@/workflow/common/type'

/**
 * 按 HTTP 入参配置（nodeData.meta）计算开始节点输出字段，
 * 与设置面板的 updateFieldList、后端 ProcessorStartNode.write 的运行时上下文保持一致：
 * 每个查询/路径参数按参数名平铺一个字段；JSON 请求体为 body，
 * 表单请求为各文件字段 + formAttributes。
 */
export function computeHttpFieldList(meta: any): Array<{ label: string; value: string }> {
  if (!meta) {
    // 尚未配置 HTTP 入参：按默认 application/json 兜底
    return [{ label: '请求体', value: 'body' }]
  }
  const fieldList: Array<{ label: string; value: string }> = (meta.parameters ?? []).map(
    (item: any) => ({ label: item.description, value: item.field })
  )
  if ((meta.contentType ?? 'application/json') === 'application/json') {
    fieldList.push({ label: 'body', value: 'body' })
  } else {
    ;(meta.requestBody ?? [])
      .filter((item: any) => item.type === 'file')
      .forEach((item: any) => fieldList.push({ label: item.description, value: item.field }))
    fieldList.push({ label: 'formAttributes', value: 'formAttributes' })
  }
  return fieldList
}

export function init(ctx: NodeInitContext) {
  ctx.model.properties.field_list = computeHttpFieldList(ctx.model.properties.nodeData?.meta)
}
