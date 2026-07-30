import { z } from 'zod'
import type { ValidationResult } from './type'

/**
 * 归一化 required 真值：节点数据存库后 required 会以字符串回流(后端为 String 类型),
 * 而 JS 中 "false" 是真值,直接用裸真值判断会把"非必填"误判为"必填"。
 */
export const isRequired = (v: any): boolean => v === true || v === 'true'

/**
 * 深度收集校验错误,带上字段路径。兼容:字符串 / PrimeVue 的 [{message}] / {message} /
 * 按字段名嵌套的结构。PrimeVue Forms 的 validate() 会把"无错字段"也列进 errors(值为空数组/空对象),
 * 直接用 Object.keys(errors).length 判断会误判校验不过,故改为按是否收集到真实 message 判定。
 */
export const collectFieldErrors = (errors: any): Array<{ field: string; message: string }> => {
  const out: Array<{ field: string; message: string }> = []
  const walk = (node: any, field: string) => {
    if (node === null || node === undefined) return
    if (typeof node === 'string') {
      if (node.trim()) out.push({ field, message: node })
      return
    }
    if (Array.isArray(node)) {
      node.forEach((n) => walk(n, field))
      return
    }
    if (typeof node === 'object') {
      if (typeof node.message === 'string' && node.message.trim()) {
        out.push({ field, message: node.message })
        return
      }
      for (const k of Object.keys(node)) {
        if (k === 'message') continue
        walk(node[k], field ? `${field}.${k}` : k)
      }
    }
  }
  for (const k of Object.keys(errors || {})) walk(errors[k], k)
  return out
}

/** 仅取错误信息文本(用于"是否有真实错误"的判定) */
export const collectErrorMessages = (errors: any): string[] =>
  collectFieldErrors(errors).map((e) => e.message)

export function parseZodResult(result: z.ZodSafeParseResult<any>): ValidationResult {
  if (result.success) {
    return { valid: true, errors: {} }
  }
  const errors: Record<string, string> = {}
  for (const issue of result.error.issues) {
    const key = issue.path.join('.') || '_root'
    if (!errors[key]) {
      errors[key] = issue.message
    }
  }
  return { valid: false, errors }
}
