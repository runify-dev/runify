import { buildCatalogSummary } from '../../node-catalog'
import { AGENT_TOOL_NODE_MAP } from '../../agent-tools'
import { buildBasePrompt } from '../prompt-base'
import { processorCatalog } from './catalog'

const AGENT_TOOL_MAPPING_DOC = Object.entries(AGENT_TOOL_NODE_MAP)
  .map(([functionName, nodeType]) => `${functionName}↔${nodeType}`)
  .join('、')

/** 处理器画布的常见方案参考 */
const RECIPES = `## 常见方案参考（规划时可借鉴，按需求组合变体）
- 先设计 HTTP 契约：根据需求确定请求方式/路径/入参，用 update_node(nodeId="start-node") 配置
  开始节点（如查询列表 GET /users、查单个 GET /users/:id + path 参数、创建 POST /users + JSON body），
  再搭建后续流程；下游节点按开始节点返回的 field_list 引用入参。
- 涉及数据库的方案，写 SQL 前必须先摸清库结构：get_database_pools 选连接池 →
  get_database_tables 查真实表名 → get_database_columns 查列名；禁止凭猜测编表名/列名。
  并按连接池的 provider（mysql / postgresql）写对应方言的 SQL——两者引号、UPSERT、大小写等语法不同，
  详见 database-search / database-insert 节点文档。
- 数据查询接口：start → database-search（SQL 模板 #{参数} 经 parameters 引用 start-node 的入参字段）
  → response（jsonFields/jsonObject 输出查询结果）
- 数据写入接口：start → database-insert（参数引用 body 字段）→ response（返回 affectedRows / 处理结果）
- 缓存加速查询：start → cache-query → judge（result 用 is_null 判断）→ 未命中分支 database-search →
  cache-write（写回缓存，设置 ttl）→ response；命中分支直接 response 缓存值
- AI 数据加工接口：start → database-search / knowledge-search 取数 → java-script/extract 预处理 →
  ai-chat（system 引用数据变量，整理/总结/抽取）→ response 输出结构化结果
- 工具增强处理（agent 变体）：ai-chat 定义标准工具函数（tools.tools 每项只写
  {"type":"function","function":{"name":"标准名"}}，系统自动替换完整定义，严禁改名或自编参数；
  标准名与工具节点一一对应：${AGENT_TOOL_MAPPING_DOC}）→ judge（finishReason=="tool_calls"）→
  loop-node(foreach, loopVariable=[aiChatID,"toolCalls"]) 内 extract 提取 functionName → judge 分流 →
  各工具节点（location="tool_call"，reference=[循环ID,"item"]）；如需把工具结果回喂模型多轮迭代，
  用外层 infinite 循环 + 循环变量 context + context-push 组合（参考各节点文档）。
- 处理器没有对话会话：context-query-node / context-save-node 依赖会话标识，处理器场景不要使用。
- 最终必须有 response-node 把结果返回给 HTTP 调用方，否则接口无响应内容。`

export function buildProcessorPrompt(): string {
  return buildBasePrompt({
    scene: '「项目处理器工作流」（对外提供 HTTP API：接收 HTTP 请求，经工作流处理后由 response 节点返回响应）',
    startNodeRule:
      '- 画布上始终存在开始节点，id 固定为 "start-node"，不可添加/删除，但它的 HTTP 入参——请求方式、\n' +
      '  请求路径、查询/路径参数、请求体类型——可以且应当由你通过 update_node(nodeId="start-node") 按需求\n' +
      '  配置（详细结构先 get_node_schema 查 start-node；修改会同步保存到处理器配置并刷新输出字段）。\n' +
      '  它的输出字段由入参配置决定（每个参数一个同名字段，JSON 请求体为 body，表单请求为 formAttributes\n' +
      '  与各文件字段），以 update_node 返回或画布快照中 start-node 的 field_list 为准。\n' +
      '  引用 JSON 请求体内的具体字段可用深层路径，如 ["start-node","body","userId"]。',
    loopRule:
      '- 支持循环：添加 loop-node 后，用 parentLoopId 在其子画布内添加节点/连线；\n' +
      '  子画布入口固定为 "loop-start-node"，第一条边必须从它引出。',
    extraCanvasRules: [
      '- 处理器画布的循环不可嵌套（loop-node 不能放进循环子画布）；database-search-node、database-insert-node、',
      '  cache-query-node、java-script-node 只能放在主画布，不能放进循环子画布。'
    ],
    variableRefExample: '["start-node","body"]（JSON 内具体字段可加层级，如 ["start-node","body","userId"]）',
    catalogSummary: buildCatalogSummary(processorCatalog),
    resourceHint:
      '；get_database_pools 只在方案确定用到数据库/缓存节点时调用，' +
      '涉及 SQL 时再用 get_database_tables / get_database_columns 查清真实表名列名',
    recipes: RECIPES,
    extraForbiddenIds: '、连接池 id'
  })
}
