import { buildCatalogSummary } from '../../node-catalog'
import { AGENT_TOOL_NODE_MAP } from '../../agent-tools'
import { buildBasePrompt } from '../prompt-base'
import { applicationCatalog } from './catalog'

const AGENT_TOOL_MAPPING_DOC = Object.entries(AGENT_TOOL_NODE_MAP)
  .map(([functionName, nodeType]) => `${functionName}↔${nodeType}`)
  .join('、')

/** 应用画布的常见方案参考（含 Agent 标准蓝图） */
const RECIPES = `## 常见方案参考（规划时可借鉴，按需求组合变体）
- 知识库问答：start → knowledge-search（keyword 引用 start-node.question）→ ai-chat
  （system 中嵌入检索结果变量，基于资料回答）
- 意图分流：start → ai-chat（分类，限定只输出类别词）→ judge（按分类结果分支）→ 各分支独立处理链
- 资料加工管道：读取/检索类节点 → java-script/extract 加工数据 → ai-chat 总结 → response 输出结构化结果
- Agent（工具增强对话）的标准蓝图，按此结构搭建：
  1) 主画布：start → context-query（载入持久化上下文）→ 外层 loop-node(infinite) → context-save
     （summaryReference/factsReference 引用外层循环的 summary/facts）
  2) 外层循环 loopVariables 逐字使用（注意 dataType 必须准确）：
     [{"name":"context","label":"上下文","dataType":"array","defaultValue":"[]"},
      {"name":"summary","label":"摘要状态","dataType":"dict","defaultValue":"{}"},
      {"name":"facts","label":"便签","dataType":"array","defaultValue":"[]"},
      {"name":"compress_context","label":"压缩后的上下文","dataType":"array","defaultValue":"[]"}]
  3) 外层循环体：loop-start-node → context-manage（sourceSeedVariable=[外层循环ID,"context"]，
     sourceVariable=[外层循环ID,"compress_context"]，summary/facts 的 seed 取 context-query 输出、
     出参写回循环变量，enableSummarizer=true 时填 summarizerModelId）→ ai-chat（enableContext=true，
     contextVariable=[外层循环ID,"compress_context"]，tools 里定义全部工具函数，函数名与工具节点对应）
     → judge：finishReason == "tool_calls" 走内层循环分支，else 分支 → loop-break（回答完成，跳出）
  4) 内层 loop-node(foreach, loopVariable=[aiChatID,"toolCalls"])，循环体：loop-start-node →
     extract（sourceReference=[内层循环ID,"item"]，rules 提取 functionName)）→ judge 按 functionName
     逐分支分流 → 各工具节点（location="tool_call"，reference=[内层循环ID,"item"]）→ 每个工具节点后接
     context-push（把 [工具节点ID,"tool"] 以 role="user" 推送到 [外层循环ID,"context"]）
  5) ai-chat 的 tools 必须使用系统预置的标准工具定义：tools.tools 里每项只写
     {"type":"function","function":{"name":"标准名"}}，系统自动替换为完整标准定义。
     严禁修改工具名称或自编参数 schema——后端工具节点按标准函数名/参数名解析，改了就全部错乱。
     标准名与工具节点一一对应：${AGENT_TOOL_MAPPING_DOC}。
     只为方案中实际放置的工具节点声明对应函数，两边必须一一对应。
  6) ai-chat 的 system 提示词按「角色目标 / 工作方法 / 工具契约 / 文件交付契约 / 何时结束」组织，
     文件交付契约见下节，必须写入。`

export function buildApplicationPrompt(): string {
  return buildBasePrompt({
    scene: '「对话应用工作流」',
    startNodeRule:
      '- 画布上始终存在开始节点，id 固定为 "start-node"，不可修改/删除，它的输出字段：messages（上下文）、question（用户问题）。',
    loopRule:
      '- 支持循环：添加 loop-node 后，用 parentLoopId 在其子画布内添加节点/连线；\n' +
      '  子画布入口固定为 "loop-start-node"，第一条边必须从它引出；循环可嵌套。',
    variableRefExample: '["start-node","question"]',
    catalogSummary: buildCatalogSummary(applicationCatalog),
    recipes: RECIPES,
    startNodeValidateRule:
      '若返回 startNodeErrors（开始节点配置\n' +
      '   缺失/有误），这是用户职责且你无权修改：不要反复重试，继续完成其余部分，\n' +
      '   并在 finish 总结中提醒用户按提示到开始节点完成配置。'
  })
}
