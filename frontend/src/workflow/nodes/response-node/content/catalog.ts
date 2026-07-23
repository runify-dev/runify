import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { catalog as applicationCatalog } from './application/catalog'
import { catalog as processorCatalog } from './processor/catalog'

/** response 是画布差异节点：与 content/{application,processor} 的校验器分裂一一对应 */
export const catalog: NodeCatalogDef[] = [applicationCatalog, processorCatalog]
