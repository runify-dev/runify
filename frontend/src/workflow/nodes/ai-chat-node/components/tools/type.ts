export interface Parameter {
  name: string
  type: string
  description: string
  required: boolean
}

export interface ToolFunction {
  name: string
  description: string
  parameters: {
    type: 'object'
    properties: Record<string, { type: string; description: string }>
    required?: string[]
  }
}

export interface Tool {
  type: 'function'
  function: ToolFunction
}

export interface ToolsConfig {
  location: 'reference' | 'customize'
  reference?: string[]
  tools?: Tool[]
}

export const paramTypes = [
  { label: 'string', value: 'string' },
  { label: 'number', value: 'number' },
  { label: 'boolean', value: 'boolean' },
  { label: 'object', value: 'object' },
  { label: 'array', value: 'array' }
]
