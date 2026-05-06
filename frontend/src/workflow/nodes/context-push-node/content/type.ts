export interface ContextPushItem {
  variable: string[]      // 目标变量路径
  mode: 'reference' | 'custom'
  reference?: string[]    // 引用模式：从上游节点获取上下文数据
  content?: string        // 自定义模式：手动输入内容（JSON 格式）
  role: 'system' | 'user' | 'assistant' | 'tool'
}

export interface ToolCallContent {
  content: string
  functionArguments: string
  toolName: string
}

export const modeOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'custom' }
]

export const roleOptions = [
  { label: 'System', value: 'system' },
  { label: 'User', value: 'user' },
  { label: 'Assistant', value: 'assistant' },
  { label: 'Tool', value: 'tool' }
]

// Tool 角色的默认 JSON 模板
export const toolCallTemplate = JSON.stringify({
  toolName: 'get_weather',
  functionArguments: '{"city": "北京"}',
  content: '北京今天晴，气温 25°C'
}, null, 2)
