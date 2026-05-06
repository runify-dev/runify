export interface GlobalVariable {
  name: string
  label: string
  dataType: 'string' | 'number' | 'boolean' | 'array' | 'dict'
  defaultValue?: any
}

export const dataTypeOptions = [
  { label: '字符串', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: '数组', value: 'array' },
  { label: '字典', value: 'dict' }
]
