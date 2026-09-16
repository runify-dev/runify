import type { WorkflowTemplate } from './types'

/** 知识库问答（RAG）：先检索知识库，再让模型基于命中片段作答。 */
export const rag: WorkflowTemplate = {
  name: '知识库问答(RAG)',
  when: '需要基于知识库/文档回答（客服问答、资料助手等）——先检索再作答，减少臆造。',
  recipe: [
    '形态：start-node → knowledge-search-node（检索）→ ai-chat-node（基于命中片段作答）。',
    '连接：knowledge-search 接 start-node，ai-chat 接 knowledge-search。',
    '关键配置：',
    '- knowledge-search-node：选知识库（子 agent 会自行查可用知识库）；检索文本引用用户问题 start-node.question。输出 hits（命中片段列表）、total、topScore。',
    '- ai-chat-node：user 提示词里既引用用户问题 start-node.question，也把 knowledge-search 的 hits 作为参考资料引用（:::variable {value="<知识检索节点ID>.hits" ...}:::）；system 交代"仅依据参考资料回答，资料中没有就如实说不知道"。',
    '建议步骤：',
    '1. 建 knowledge-search-node 接 start-node，配好知识库与检索文本（引 start-node.question）。',
    '2. 建 ai-chat-node 接 knowledge-search，配 system/user（引用 start-node.question 与检索节点的 hits）。'
  ].join('\n')
}
