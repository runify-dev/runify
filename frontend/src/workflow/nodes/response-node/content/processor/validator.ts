import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult, isRequired } from '@/workflow/common/validator-utils'

const handlerItemSchema = z.object({
  field: z.string().nullish(),
  value: z.any().optional(),
  location: z.string().nullish(),
  reference: z.any().optional(),
  required: z.any().optional()
})

const parameterItemSchema = z.object({
  field: z.string().nullish(),
  value: z.any().optional(),
  location: z.string().nullish(),
  reference: z.any().optional(),
  required: z.any().optional()
})

const jsonObjectSchema = z.object({
  location: z.string().nullish(),
  value: z.string().nullish(),
  reference: z.any().optional()
})

const plainTextSchema = z.object({
  location: z.string().nullish(),
  value: z.string().nullish(),
  reference: z.any().optional()
})

export const schema = z
  .object({
    status: z.number({ error: '请输入状态码' }),
    headers: z.array(handlerItemSchema).optional(),
    contentType: z.string().nullish(),
    jsonFields: z.array(parameterItemSchema).optional(),
    jsonObject: jsonObjectSchema.optional(),
    plainText: plainTextSchema.optional()
  })
  .check((ctx) => {
    const contentType = ctx.value.contentType || 'jsonFields'
    if (contentType === 'jsonFields' && Array.isArray(ctx.value.jsonFields)) {
      ctx.value.jsonFields.forEach((p: any, i: number) => {
        // 完全按「是否必填」设置校验:必填时,引用需选变量、自定义需填值;非必填留空放行
        if (isRequired(p.required)) {
          if (p.location === 'reference' && (!Array.isArray(p.reference) || p.reference.length === 0)) {
            ctx.issues.push({ code: 'custom', input: p, message: `请输入${p.field}`, path: ['jsonFields', i] })
          } else if (p.location !== 'reference' && (!p.value || String(p.value).trim() === '')) {
            ctx.issues.push({ code: 'custom', input: p, message: `请输入${p.field}`, path: ['jsonFields', i] })
          }
        }
      })
    }
    if (contentType === 'jsonObject') {
      const obj = ctx.value.jsonObject as any
      if (!obj || (obj.location !== 'reference' && (!obj.value || String(obj.value).trim() === ''))) {
        ctx.issues.push({ code: 'custom', input: obj, message: '请输入JSON内容', path: ['jsonObject'] })
      }
    }
    if (contentType === 'plainText') {
      const txt = ctx.value.plainText as any
      if (!txt || (txt.location !== 'reference' && (!txt.value || String(txt.value).trim() === ''))) {
        ctx.issues.push({ code: 'custom', input: txt, message: '请输入文本内容', path: ['plainText'] })
      }
    }
    if (Array.isArray(ctx.value.headers)) {
      ctx.value.headers.forEach((h: any, i: number) => {
        if (isRequired(h.required)) {
          if (h.location === 'reference' && (!Array.isArray(h.reference) || h.reference.length === 0)) {
            ctx.issues.push({ code: 'custom', input: h, message: `请输入${h.field}`, path: ['headers', i] })
          } else if (h.location !== 'reference' && (!h.value || String(h.value).trim() === '')) {
            ctx.issues.push({ code: 'custom', input: h, message: `请输入${h.field}`, path: ['headers', i] })
          }
        }
      })
    }
  })

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  return parseZodResult(schema.safeParse(nodeData ?? {}))
}
