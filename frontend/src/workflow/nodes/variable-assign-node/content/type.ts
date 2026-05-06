export interface VariableItem {
  variable: string[]    // 目标变量路径
  type: 'reference' | 'constant'
  reference?: string[]  // 引用变量路径（type=reference时）
  dataType?: 'string' | 'array' | 'dict' | 'number' | 'boolean'  // 数据类型（type=constant时）
  value?: any           // 常量值（type=constant时）
}

export const valueTypeOptions = [
  { label: '引用', value: 'reference' },
  { label: '常量', value: 'constant' }
]

export const dataTypeOptions = [
  { label: '字符串', value: 'string' },
  { label: '数组', value: 'array' },
  { label: '字典', value: 'dict' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' }
]
