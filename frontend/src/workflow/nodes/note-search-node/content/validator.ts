import { z } from 'zod'
import type { ValidationResult } from '@/workflow/common/type'
import { parseZodResult } from '@/workflow/common/validator-utils'

export const schema = z.object({
  folderIds: z.array(z.string()).min(1, '请选择目录'),
  keywordLocation: z.enum(['reference', 'customize']).default('customize'),
  keywordReference: z.array(z.string()).optional(),
  keyword: z.string().optional(),
  pageNoLocation: z.enum(['reference', 'customize']).default('customize'),
  pageNoReference: z.array(z.string()).optional(),
  pageNo: z.number().min(1).default(1),
  pageSizeLocation: z.enum(['reference', 'customize']).default('customize'),
  pageSizeReference: z.array(z.string()).optional(),
  pageSize: z.number().min(1).max(100).default(10)
}).refine(
  (data) => {
    if (data.keywordLocation === 'reference') {
      return data.keywordReference && data.keywordReference.length > 0
    }
    return data.keyword && data.keyword.trim().length > 0
  },
  { message: '请输入或引用检索文本', path: ['keyword'] }
)

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  if (!nodeData) return { valid: true, errors: {} }
  return parseZodResult(schema.safeParse(nodeData))
}
