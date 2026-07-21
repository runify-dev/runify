import { WorkflowType } from '@/workflow/common/data'
import { buildCatalog } from '../../node-catalog'
import { collectCatalogEntries } from '../../catalog-registry'

/** 应用（对话）画布节点目录：自动收集各节点目录 catalog.ts 中声明支持本画布的条目 */
export const applicationCatalog = buildCatalog(collectCatalogEntries(WorkflowType.APPLICATION))
