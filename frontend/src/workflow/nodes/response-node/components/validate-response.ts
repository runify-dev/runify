const isBlank = (v: any) => v === undefined || v === null || String(v).trim() === ''

/**
 * 校验响应配置(状态码/响应头/响应体),返回错误信息列表(空数组为通过)。
 * 供响应形态的配置共用:开始节点错误响应 Dialog、项目统一异常配置页。
 */
export const validateResponseConfig = (data: any): string[] => {
  const errors: string[] = []
  if (!Number.isInteger(data.status) || data.status < 100 || data.status > 599) {
    errors.push('状态码需在 100-599 之间')
  }
  for (const header of data.headers || []) {
    if (header.location === 'reference') {
      if (!header.reference?.length) errors.push(`响应头 ${header.field} 未选择引用变量`)
    } else if (isBlank(header.value)) {
      errors.push(`响应头 ${header.field} 未填写值`)
    }
  }
  if (data.contentType === 'jsonFields') {
    for (const field of data.jsonFields || []) {
      if (field.location === 'reference' && !field.reference?.length) {
        errors.push(`字段 ${field.field} 未选择引用变量`)
      }
    }
  } else if (data.contentType === 'jsonObject') {
    const jo = data.jsonObject || {}
    if (jo.location === 'reference') {
      if (!jo.reference?.length) errors.push('请选择 JSON 引用变量')
    } else if (isBlank(jo.value)) {
      errors.push('请输入 JSON 内容')
    } else {
      try {
        JSON.parse(jo.value)
      } catch {
        errors.push('JSON 格式不合法')
      }
    }
  } else if (data.contentType === 'plainText') {
    const pt = data.plainText || {}
    if (pt.location === 'reference') {
      if (!pt.reference?.length) errors.push('请选择文本引用变量')
    } else if (isBlank(pt.value)) {
      errors.push('请输入文本内容')
    }
  } else {
    errors.push('请选择响应类型')
  }
  return errors
}
