export interface CodeConfig {
  location: 'reference' | 'customize'
  reference?: string[]
  code?: string
}

export interface TimeoutConfig {
  timeoutLocation: 'reference' | 'customize'
  timeoutReference?: string[]
  timeout?: number
}

export const locationOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'customize' }
]
