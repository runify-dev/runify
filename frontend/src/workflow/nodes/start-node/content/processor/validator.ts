import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

const METHODS = ['GET', 'POST', 'PUT', 'DELETE'] as const
const PARAM_LOCATIONS = ['query', 'path'] as const
const PARAM_TYPES = ['string', 'integer', 'uuid', 'long', 'double'] as const
const CONTENT_TYPES = ['application/json', 'multipart/form-data'] as const

const parameterSchema = z.object({
  field: z.string().min(1, { error: '请输入参数名' }),
  description: z.string().min(1, { error: '请输入参数描述' }),
  location: z.enum(PARAM_LOCATIONS, { error: '参数位置只能是 query 或 path' }),
  type: z.enum(PARAM_TYPES, { error: '参数类型不合法' }),
  required: z.boolean().optional(),
  many: z.boolean().optional()
})

/** 从请求地址提取 vertx 风格路径参数（/user/:id → id） */
function extractPathParams(path: string): Set<string> {
  return new Set((path.match(/:([A-Za-z0-9_]+)/g) ?? []).map((s) => s.slice(1)))
}

export const httpSchema = z
  .object({
    method: z.enum(METHODS, { error: '请选择请求方式' }),
    path: z
      .string()
      .min(1, { error: '请输入请求地址' })
      .regex(/^\//, { error: '请求地址必须以 / 开头' })
      .refine((p) => !/\s/.test(p), { error: '请求地址不能包含空格' }),
    contentType: z.enum(CONTENT_TYPES, { error: '请求类型不合法' }).optional(),
    parameters: z.array(parameterSchema).optional(),
    requestBody: z.array(z.any()).optional(),
    errorResponseSource: z.string().optional(),
    errorResponse: z.any().optional()
  })
  .check((ctx) => {
    const meta = ctx.value
    const path = typeof meta.path === 'string' ? meta.path : ''
    const parameters: any[] = Array.isArray(meta.parameters) ? meta.parameters : []

    // 参数重名（后端按参数名平铺写入上下文，重名互相覆盖）
    const seen = new Set<string>()
    parameters.forEach((p, i) => {
      if (!p?.field) return
      if (seen.has(p.field)) {
        ctx.issues.push({ code: 'custom', input: p, message: `参数名重复：${p.field}`, path: ['parameters', i, 'field'] })
      }
      seen.add(p.field)
    })

    // 路径参数与请求地址双向对应（后端 routingContext.pathParam(field) 依赖 :field 占位）
    const pathParams = extractPathParams(path)
    parameters.forEach((p, i) => {
      if (p?.location === 'path' && p?.field && !pathParams.has(p.field)) {
        ctx.issues.push({
          code: 'custom',
          input: p,
          message: `路径参数 ${p.field} 未出现在请求地址中（地址应形如 /xxx/:${p.field}）`,
          path: ['parameters', i, 'field']
        })
      }
    })
    const declared = new Set(parameters.filter((p) => p?.location === 'path').map((p) => p.field))
    for (const seg of pathParams) {
      if (!declared.has(seg)) {
        ctx.issues.push({
          code: 'custom',
          input: path,
          message: `请求地址中的路径参数 :${seg} 未在参数列表中声明（位置选 Path）`,
          path: ['path']
        })
      }
    }

    // multipart/form-data 请求体字段：名称必填且不重复
    if (meta.contentType === 'multipart/form-data') {
      const bodySeen = new Set<string>()
      const requestBody: any[] = Array.isArray(meta.requestBody) ? meta.requestBody : []
      requestBody.forEach((item, i) => {
        if (!item || typeof item !== 'object') return
        if (!item.field || String(item.field).trim() === '') {
          ctx.issues.push({ code: 'custom', input: item, message: '请输入请求体字段名', path: ['requestBody', i, 'field'] })
          return
        }
        if (bodySeen.has(item.field)) {
          ctx.issues.push({ code: 'custom', input: item, message: `请求体字段名重复：${item.field}`, path: ['requestBody', i, 'field'] })
        }
        bodySeen.add(item.field)
      })
    }

    // 错误响应结构兜底（主校验在配置 Dialog 提交时；此处拦截绕过 UI 的脏数据,如 AI 生成）
    // 仅自定义来源需要校验;global(默认)走项目统一配置,结构由统一配置页保证
    const er: any = (meta as any).errorResponse
    if ((meta as any).errorResponseSource === 'custom' && er) {
      const erIssue = (message: string, subPath: Array<string | number>) => {
        ctx.issues.push({ code: 'custom', input: er, message, path: ['errorResponse', ...subPath] })
      }
      if (!Number.isInteger(er.status) || er.status < 100 || er.status > 599) {
        erIssue('错误响应状态码需在 100-599 之间', ['status'])
      }
      if (!['jsonFields', 'jsonObject', 'plainText'].includes(er.contentType)) {
        erIssue('错误响应类型不合法', ['contentType'])
      }
      const erBlank = (v: any) => v === undefined || v === null || String(v).trim() === ''
      ;(Array.isArray(er.headers) ? er.headers : []).forEach((h: any, i: number) => {
        if (h?.location === 'reference') {
          if (!h.reference?.length) erIssue(`错误响应头 ${h.field} 未选择引用变量`, ['headers', i])
        } else if (erBlank(h?.value)) {
          erIssue(`错误响应头 ${h?.field} 未填写值`, ['headers', i])
        }
      })
      if (er.contentType === 'jsonFields') {
        ;(Array.isArray(er.jsonFields) ? er.jsonFields : []).forEach((f: any, i: number) => {
          if (f?.location === 'reference' && !f.reference?.length) {
            erIssue(`错误响应字段 ${f.field} 未选择引用变量`, ['jsonFields', i])
          }
        })
      } else if (er.contentType === 'jsonObject') {
        const jo = er.jsonObject || {}
        if (jo.location === 'reference') {
          if (!jo.reference?.length) erIssue('错误响应未选择 JSON 引用变量', ['jsonObject'])
        } else if (erBlank(jo.value)) {
          erIssue('错误响应未填写 JSON 内容', ['jsonObject'])
        } else {
          try {
            JSON.parse(jo.value)
          } catch {
            erIssue('错误响应 JSON 格式不合法', ['jsonObject'])
          }
        }
      } else if (er.contentType === 'plainText') {
        const pt = er.plainText || {}
        if (pt.location === 'reference') {
          if (!pt.reference?.length) erIssue('错误响应未选择文本引用变量', ['plainText'])
        } else if (erBlank(pt.value)) {
          erIssue('错误响应未填写文本内容', ['plainText'])
        }
      }
    }
  })

const validators: Record<string, (nodeData: any) => ValidationResult> = {
  HTTP: (nodeData) => parseZodResult(httpSchema.safeParse(nodeData?.meta ?? {}))
}

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  // 未配置过 HTTP 入参（nodeData/meta 为空）时后端反序列化会直接 NPE，必须拦下
  if (!nodeData || !nodeData.protocol || !nodeData.meta) {
    return { valid: false, errors: { meta: '请配置开始节点的 HTTP 入参（请求方式与请求地址）' } }
  }
  return validators[nodeData.protocol]?.(nodeData) ?? { valid: true, errors: {} }
}
