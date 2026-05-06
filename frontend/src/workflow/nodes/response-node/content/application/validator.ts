import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

const parameterItemSchema = z.object({
  field: z.string(),
  value: z.any(),
  location: z.string(),
  reference: z.any().optional(),
  required: z.boolean().optional()
})

const jsonObjectSchema = z.object({
  location: z.string(),
  value: z.string().optional(),
  reference: z.any().optional()
})

const plainTextSchema = z.object({
  location: z.string(),
  value: z.string().optional(),
  reference: z.any().optional()
})

export const schema = z
  .object({
    contentType: z.string().optional(),
    jsonFields: z.array(parameterItemSchema).optional(),
    jsonObject: jsonObjectSchema.optional(),
    plainText: plainTextSchema.optional()
  })
  .check((ctx) => {
    const contentType = ctx.value.contentType || 'jsonFields'
    if (contentType === 'jsonFields' && Array.isArray(ctx.value.jsonFields)) {
      ctx.value.jsonFields.forEach((p: any, i: number) => {
        if (p.required && p.location === 'reference' && (!Array.isArray(p.reference) || p.reference.length === 0)) {
          ctx.issues.push({ code: 'custom', input: p, message: `请输入${p.field}`, path: ['jsonFields', i] })
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
  })

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  return parseZodResult(schema.safeParse(nodeData ?? {}))
}
