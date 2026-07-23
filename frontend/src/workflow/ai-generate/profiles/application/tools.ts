import type { ToolExecution } from '../../types'

/**
 * 应用（对话）画布专属工具：目前无。
 * 保留本文件是为了让每个 profile 目录结构一致（index/catalog/prompt/tools 四件套）；
 * 将来该画布需要专属工具（执行器 + schema）时在此补充。
 */
export const applicationToolSchemas: any[] = []
export const applicationToolExecutors: Record<string, ToolExecution> = {}
