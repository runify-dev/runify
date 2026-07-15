export const summarizerMethodOptions = [
  { label: '工具调用（默认，结构可靠）', value: 'fc' },
  { label: '提示词（兼容无工具调用能力的模型）', value: 'prompt' }
]

export const factSectionOptions = [
  { label: '约定', value: 'convention' },
  { label: '喜好', value: 'preference' },
  { label: '环境', value: 'env' },
  { label: '目标', value: 'goal' },
  { label: '待办', value: 'todo' }
]

export const tokenEncodingOptions = [
  { label: 'cl100k（GPT-4/deepseek 近似）', value: 'cl100k' },
  { label: 'o200k（GPT-4o 系）', value: 'o200k' }
]
