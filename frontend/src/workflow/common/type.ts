import type { BaseNodeModel } from '@logicflow/core'

interface Field {
  label: string,
  value: string
}
interface LifeCycle {
  onMounted?: () => void
  onBeforeMount?: () => void
  onBeforeUnmount?: () => void
}
interface ValidationResult {
  valid: boolean
  errors: Record<string, string>
  failedNodeId?: string
  failedPath?: string[]
}
interface NodeInitContext {
  model: BaseNodeModel
  workflowType: string
  details?: any
}
export { type Field, type LifeCycle, type ValidationResult, type NodeInitContext }
