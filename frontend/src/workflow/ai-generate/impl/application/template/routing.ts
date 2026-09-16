import type { WorkflowTemplate } from './types'

/** 意图分流：先分类用户意图，再按类别走各自的处理链。 */
export const routing: WorkflowTemplate = {
  name: '意图分流',
  when: '一个入口要处理多种不同类型的请求（如客服区分「售前咨询/售后报修/闲聊」），需要先判定意图再分别处理。',
  recipe: [
    '形态：start-node → ai-chat（意图分类）→ judge-node（按分类结果分支）→ 各分支各自独立的处理链。',
    '连接：ai-chat 接 start-node；judge 接 ai-chat；judge 的每条出边接一条独立处理链（分类命中什么就走哪条）。',
    '关键配置：',
    '- 分类用的 ai-chat：system 限定它「只输出类别词」（如只回 售前/售后/闲聊 之一），user 引用 start-node.question；输出 content 即类别。',
    '- judge-node：按上一步 ai-chat 的 content 做分支匹配，每个类别一条出边；每条出边必须指定分支。',
    '- 各分支处理链：按该类别需求组合节点（可以是简单 ai-chat 回答，也可嵌套知识检索/工具等其它范式）。',
    '建议步骤：',
    '1. 建分类 ai-chat 接 start-node，system 限定只输出类别词。',
    '2. 建 judge 接分类 ai-chat，按类别设分支。',
    '3. 为每个分支建各自的处理链（可复用简单问答/RAG 等范式）。'
  ].join('\n')
}
