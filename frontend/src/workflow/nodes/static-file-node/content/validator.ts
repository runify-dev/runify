import type { ValidationResult } from '@/workflow/common/type'

export function validate(nodeData: Record<string, any> | undefined): ValidationResult {
  const data = nodeData ?? {}

  if (!data.fileId) {
    return { valid: false, errors: { file: '请上传ZIP文件' } }
  }

  return { valid: true, errors: {} }
}
