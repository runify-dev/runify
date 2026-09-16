import type { WorkflowTemplate } from './types'

/** 简单问答：一问一答 / 内容生成，直接用 ai-chat 流式回答。 */
export const simpleChat: WorkflowTemplate = {
  name: '简单问答',
  when: '一问一答、翻译、润色、内容生成等——无需检索知识库、无需多步自主决策/工具调用。',
  recipe: [
    '形态：start-node → ai-chat-node。ai-chat 的回答直接流式返回给用户，即为最终响应，通常无需 response-node。',
    '连接：ai-chat 接在 start-node 之后。',
    '关键配置：',
    '- ai-chat-node：选一个模型；user 提示词里用变量指令引用用户问题 :::variable {value="start-node.question" label="用户问题"}:::；按任务写 system（如"你是翻译助手，把用户输入的中文翻译成英文"）。',
    '建议步骤（单步即可）：',
    '1. 建 ai-chat-node 接 start-node，并配好 system/user（引用 start-node.question）。'
  ].join('\n')
}
