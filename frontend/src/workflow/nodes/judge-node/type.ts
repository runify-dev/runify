import { randomId } from '@/utils/common'

export type BranchType = 'if' | 'elseif' | 'else'

export type BranchLogic = 'and' | 'or'

export type ValueMode = 'str' | 'var'

export type CompareValue =
  | 'is_null'
  | 'is_not_null'
  | 'contain'
  | 'not_contain'
  | 'eq'
  | 'not_eq'
  | 'ge'
  | 'gt'
  | 'le'
  | 'lt'
  | 'len_eq'
  | 'len_ge'
  | 'len_gt'
  | 'len_le'
  | 'len_lt'
  | 'is_true'
  | 'is_not_true'
  | 'start_with'
  | 'end_with'
  | 'regex'
  | 'wildcard'

export interface JudgeCondition {
  id: string
  variable: string[]
  compare?: CompareValue
  value?: string
}

export interface JudgeBranch {
  id: string
  type: BranchType
  logic?: BranchLogic
  conditions?: JudgeCondition[]
}

export interface CascaderOption {
  label: string
  value: string
  disabled?: boolean
  children?: CascaderOption[]
  [key: string]: any
}

export interface ConditionError {
  variable: boolean
  compare: boolean
  value: boolean
}

export interface DisplayConditionError {
  left: boolean
  compare: boolean
  right: boolean
  message: string
}

export interface FormErrorItem {
  message: string
}

export type FormErrors = Record<string, FormErrorItem[]>

export interface FormResult<T = any> {
  valid: boolean
  errors: FormErrors
  values: T
}

export const compareOptions: Array<{ label: string; value: CompareValue }> = [
  { value: 'eq', label: '=' },
  { value: 'not_eq', label: '!=' },
  { value: 'ge', label: '>=' },
  { value: 'gt', label: '>' },
  { value: 'le', label: '<=' },
  { value: 'lt', label: '<' },
  { value: 'contain', label: '包含' },
  { value: 'not_contain', label: '不含' },
  { value: 'is_null', label: '为空' },
  { value: 'is_not_null', label: '非空' },
  { value: 'len_eq', label: '长度=' },
  { value: 'len_ge', label: '长度>=' },
  { value: 'len_gt', label: '长度>' },
  { value: 'len_le', label: '长度<=' },
  { value: 'len_lt', label: '长度<' },
  { value: 'is_true', label: '为真' },
  { value: 'is_not_true', label: '非真' },
  { value: 'start_with', label: '开头' },
  { value: 'end_with', label: '结尾' },
  { value: 'regex', label: '正则' },
  { value: 'wildcard', label: '通配' }
]

export const compareTextMap: Record<CompareValue, string> = compareOptions.reduce(
  (result, item) => {
    result[item.value] = item.label
    return result
  },
  {} as Record<CompareValue, string>
)

export const logicOptions: Array<{ label: string; value: BranchLogic }> = [
  { label: '且', value: 'and' },
  { label: '或', value: 'or' }
]

export const valueModeOptions: Array<{ label: string; value: ValueMode }> = [
  { label: 'str', value: 'str' },
  { label: 'var', value: 'var' }
]

export const noRightValueCompares: CompareValue[] = [
  'is_null',
  'is_not_null',
  'is_true',
  'is_not_true'
]

export function createCondition(): JudgeCondition {
  return {
    id: randomId(),
    variable: [],
    compare: undefined,
    value: ''
  }
}

export function createBranch(type: BranchType): JudgeBranch {
  if (type === 'else') {
    return {
      id: randomId(),
      type
    }
  }

  return {
    id: randomId(),
    type,
    logic: 'and',
    conditions: [createCondition()]
  }
}

export function getBranchTitle(type: BranchType) {
  if (type === 'if') {
    return 'IF'
  }

  if (type === 'elseif') {
    return 'ELIF'
  }

  return 'ELSE'
}

export function getConditions(branch: JudgeBranch): JudgeCondition[] {
  if (branch.type === 'else') {
    return []
  }

  return branch.conditions || []
}

export function ensureConditions(branch: JudgeBranch): JudgeCondition[] {
  if (branch.type === 'else') {
    return []
  }

  if (!branch.conditions || branch.conditions.length === 0) {
    branch.conditions = [createCondition()]
  }

  return branch.conditions
}

export function buildBranchesValue(branches: JudgeBranch[]): JudgeBranch[] {
  return branches.map((branch) => {
    if (branch.type === 'else') {
      return {
        id: branch.id,
        type: 'else'
      }
    }

    return {
      id: branch.id,
      type: branch.type,
      logic: branch.logic || 'and',
      conditions: getConditions(branch).map((condition) => ({
        id: condition.id,
        variable: [...condition.variable],
        compare: condition.compare,
        value: condition.value || ''
      }))
    }
  })
}

export function createFormResult(
  branches: JudgeBranch[],
  errors: FormErrors = {}
): FormResult<{ branches: JudgeBranch[] }> {
  return {
    valid: Object.keys(errors).length === 0,
    errors,
    values: {
      branches: buildBranchesValue(branches)
    }
  }
}

export function needRightValue(compare?: CompareValue) {
  if (!compare) {
    return true
  }

  return !noRightValueCompares.includes(compare)
}

export function isBlank(value: unknown) {
  return value === undefined || value === null || String(value).trim() === ''
}

export function isBlankVariable(value: unknown) {
  return !Array.isArray(value) || value.length === 0 || value.every((item) => isBlank(item))
}

export function getConditionErrors(condition: JudgeCondition): ConditionError {
  return {
    variable: isBlankVariable(condition.variable),
    compare: isBlank(condition.compare),
    value: needRightValue(condition.compare) && isBlank(condition.value)
  }
}

export function getDisplayConditionError(condition: JudgeCondition): DisplayConditionError {
  const errors = getConditionErrors(condition)

  let message = ''

  if (errors.variable) {
    message = '请选择左值'
  } else if (errors.compare) {
    message = '请选择比较符'
  } else if (errors.value) {
    message = '请输入右值'
  }

  return {
    left: errors.variable,
    compare: errors.compare,
    right: errors.value,
    message
  }
}

export function getConditionKey(branch: JudgeBranch, condition: JudgeCondition) {
  return `${branch.id}_${condition.id}`
}

export function buildValidationMap(branches: JudgeBranch[]) {
  const map: Record<string, DisplayConditionError> = {}

  branches.forEach((branch) => {
    if (branch.type === 'else') {
      return
    }

    getConditions(branch).forEach((condition) => {
      map[getConditionKey(branch, condition)] = getDisplayConditionError(condition)
    })
  })

  return map
}

export function formatVariable(variable: unknown) {
  if (!Array.isArray(variable)) {
    return ''
  }

  return variable
    .map((item) => String(item || '').trim())
    .filter(Boolean)
    .join('.')
}

export function parseVariableExpression(value?: string): string[] {
  if (!value) {
    return []
  }

  const match = value.match(/^\$\{(.+)\}$/)

  if (!match?.[1]) {
    return []
  }

  return match[1]
    .split('.')
    .map((item) => item.trim())
    .filter(Boolean)
}

export function isVariableExpression(value?: string) {
  return parseVariableExpression(value).length > 0
}

export function findOptionByPath(
  options: CascaderOption[],
  path?: string[]
): CascaderOption | null {
  if (!path || path.length === 0) {
    return null
  }

  let currentOptions = options
  let current: CascaderOption | undefined

  for (const value of path) {
    current = currentOptions.find((item) => String(item.value) === String(value))

    if (!current) {
      return null
    }

    currentOptions = current.children || []
  }

  return current || null
}

export function getCascaderOptionPath(options: CascaderOption[], selected: unknown): string[] {
  if (!selected || typeof selected !== 'object') {
    return []
  }

  const selectedOption = selected as CascaderOption

  const identityPath = findOptionPath(options, (item) => item === selectedOption)

  if (identityPath.length > 0) {
    return identityPath
  }

  return findOptionPath(options, (item) => String(item.value) === String(selectedOption.value))
}

function findOptionPath(
  options: CascaderOption[],
  matcher: (item: CascaderOption) => boolean,
  parentPath: string[] = []
): string[] {
  for (const option of options) {
    const currentPath = [...parentPath, option.value]

    if (matcher(option)) {
      return currentPath
    }

    if (option.children?.length) {
      const childPath = findOptionPath(option.children, matcher, currentPath)

      if (childPath.length > 0) {
        return childPath
      }
    }
  }

  return []
}

export function getMaxChildrenDepth(options: CascaderOption[]): number {
  if (!options.length) {
    return 0
  }

  return Math.max(
    ...options.map((item) => {
      if (!item.children?.length) {
        return 0
      }

      return 1 + getMaxChildrenDepth(item.children)
    })
  )
}

export function buildOptionGroupChildren(options: CascaderOption[]) {
  const depth = getMaxChildrenDepth(options)

  if (depth <= 0) {
    return []
  }

  return Array.from({ length: depth }, () => 'children')
}
