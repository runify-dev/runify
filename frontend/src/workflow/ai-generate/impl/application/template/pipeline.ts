import type { WorkflowTemplate } from './types'

/** 资料加工管道：取数 → 加工 → AI 总结 → 结构化输出。 */
export const pipeline: WorkflowTemplate = {
  name: '资料加工管道',
  when: '需要先取数据、做一轮加工/清洗/抽取，再由 AI 汇总，并输出结构化结果（如把检索/接口数据整理成报告或 JSON）。不需要多步自主循环。',
  recipe: [
    '形态：start-node → 取数节点（knowledge-search / database-search 等）→ 加工节点（java-script 或 extract）→ ai-chat（汇总/生成）→ response-node（结构化输出）。',
    '连接：依次串起来，每个节点接上一个。',
    '关键配置：',
    '- 取数节点：按来源选（知识库检索 knowledge-search、库查询 database-search 等），其子 agent 会自行查可用资源；检索/查询条件引用 start-node.question 或相关上游。',
    '- 加工节点：java-script-node 写脚本转换/清洗，或 extract-node 按规则抽字段；引用取数节点的输出。',
    '- ai-chat：把加工后的数据作为上下文，system 交代产出格式，生成总结/报告。',
    '- response-node：需要给调用方结构化数据时输出（对话展示则可省，用 ai-chat 流式回答收尾）。',
    '建议步骤：',
    '1. 建取数节点接 start-node，配好检索/查询条件。',
    '2. 建加工节点接取数节点，配转换/抽取规则。',
    '3. 建 ai-chat 接加工节点，配 system 与数据引用。',
    '4. 需结构化输出时建 response-node 接 ai-chat。'
  ].join('\n')
}
