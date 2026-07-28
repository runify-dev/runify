import { Result } from '@/request/Result'
import { put } from '@/request/admin/index'
import { TreeCommonAPI } from '@/api/tree'
import type { Ref } from 'vue'

/**
 * 工具运行时
 */
export type ToolRuntime = 'WORKFLOW' | 'JS' | 'PY'

export interface ToolDetail {
  id: string
  parentId: string
  name: string
  label: string
  description: string
  icon: string
  runtime: ToolRuntime
  inputSchema: any[]
  outputSchema: any[]
  configSchema: any[]
  /** 配置值(secret 已脱敏) */
  config: Record<string, any>
  /** 运行时实现体(WORKFLOW=图JSON; JS=代码等) */
  body: Record<string, any>
  createTime: string
  updateTime: string
}

export interface EditToolPojo {
  name?: string
  label?: string
  description?: string
  icon?: string
  runtime?: ToolRuntime
  inputSchema?: any[]
  outputSchema?: any[]
  configSchema?: any[]
  config?: Record<string, any>
  body?: Record<string, any>
}

const tree = new TreeCommonAPI('tool')

/**
 * 编辑工具（PUT /tool/resources/:id）
 */
const edit = (
  resourceId: string,
  pojo: EditToolPojo,
  loading?: Ref<boolean>
): Promise<Result<ToolDetail>> => {
  return put(`/tool/resources/${resourceId}`, pojo, undefined, loading)
}

export default {
  tree,
  edit,
  // 透传通用树接口
  createFolder: tree.createFolder,
  createResource: tree.createResource,
  listResource: tree.listResource,
  listTree: tree.listTree,
  getResource: tree.getResource,
  getFolder: tree.getFolder,
  modifyFolderName: tree.modifyFolderName,
  modifyResourceName: tree.modifyResourceName,
  removeFolder: tree.removeFolder,
  removeResource: tree.removeResource
}
