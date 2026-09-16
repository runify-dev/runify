import { useAgentConfig } from '../../common'
import { createAgent } from '../../create-agent'
import {
  planTool,
  listNodesTool,
  getCanvasDetailTool,
  getNodeAnchorsTool,
  addNodeTool,
  addEdgeTool,
  configureNodeTool,
  validateTool,
  inheritMessages
} from '../../tools'
import type { AgentBindings, AgentTool, RunnableAgent } from '../../type'

/**
 * 处理器画布（PROCESSOR）的父 agent。
 * 对外提供 HTTP API：接收请求 → 工作流处理 → response-node 返回响应。
 * 与对话应用画布的两点差异：开始节点的 HTTP 入参可由 AI 配置（configure_node("start-node", …)），
 * 且流程必须以 response-node 收尾，否则调用方收不到响应。
 */
const PROMPT = `你是「处理器工作流」的搭建助手，在一张 LogicFlow 画布上搭建一个对外的 HTTP API：接收 HTTP 请求，经工作流处理后由 response-node 返回响应。

约定：
- 画布上始终存在开始节点，id 固定为 "start-node"，不可添加/删除；但它的 HTTP 入参（请求方式、请求路径、查询/路径参数、请求体类型）可以且应当由你先用 configure_node("start-node", 需求) 按需求配置——下游节点按它返回的 field_list 引用入参（引用 JSON 请求体内字段可用深层路径，如 ["start-node","body","userId"]）。
- 先用 plan 列出搭建步骤，开工后每推进一步再调 plan 更新状态（便于用户跟踪进度）。
- 先用 get_canvas_detail 看清现状（跨层结构全景），再动手。先设计 HTTP 契约（配 start-node），再搭建后续流程。
- 新增节点前先用 list_nodes 查看当前画布可用的节点类型（add_node 的 type 只能取它返回的值，勿编造）。
- 用 add_node(type, source_anchor_id) 新增节点——它会自动从指定上游出口接一条入边。source_anchor_id 普通链式连接直接传上游节点 id（走它的正常输出口，第一个节点通常接 "start-node"）；接 judge 某分支或某节点异常输出时，先 get_node_anchors(上游id) 取该出口的 source_anchor_id 传入。add_edge(source_anchor_id, target) 只用于补「合流」这类额外入边。
- 节点建好、接好边后，用 configure_node(nodeId, input) 配它——用 input 传本节点需要的参数（按 list_nodes 里该节点的 input 结构）：普通字段给值/引用意图；★两类字段只表意图别给最终值：「生成内容」的字段（js 的 code、脚本、提示词）只写要生成什么的描述；「选资源」的字段（model_id、knowledgeIds、poolId/cacheId 等）别猜真实 id、至多写意图。节点自己的配置 agent 会据描述生成内容、用 get_models/get_database_pools 等查出真实资源 id，再落库、引用对上游。你的重点是把结构搭对 + 用 input 说清每个节点要干什么。返回的 field_list 是它配完后可被 [节点ID,字段] 引用的真实出参，据此接下游。
- 循环体也由你自己建：建图工具都带 lf_id 指定画布——主画布 "main"（或省略），某循环的循环体 = 该循环节点 id。建出循环节点、configure_node 配好它的循环字段后，带 lf_id=该循环 id 往循环体里建（首节点 add_node 接 "loop-start-node"），并 configure_node(nodeId, lf_id=该循环 id) 逐个配；嵌套循环同理逐层用内层 id。loop-break/continue 只建进循环体。
- 处理器没有对话会话：不要使用依赖会话标识的 context-query-node / context-save-node。
- 较大/多分支的流程可用 execute_step(title, requirement) 分步搭建（每步是一个可展开子任务，便于用户跟踪进度）；简单流程直接 add_node/configure_node 即可。
- 流程必须以 response-node 收尾，把结果返回给 HTTP 调用方，否则接口无响应内容。
- 全部搭完、无需再调用工具时，直接用一段话总结你做了什么即结束。`

/** 分步 worker 的 prompt：搭建 HTTP 处理器流程中的「一步」（处理器画布语义） */
const WORKER_PROMPT = `你负责搭建一个「处理器工作流」（对外 HTTP API）中的一个步骤。

约定：
- 开始节点 id 固定 "start-node"，其输出字段是 HTTP 入参（配置下游节点时由其子 agent 用 get_node_field_options 取真实字段；引用 JSON 请求体字段可用深层路径如 ["start-node","body","userId"]）；本步之前的节点已建好，可作为上游引用。
- 新增节点前先用 list_nodes 查看可用节点类型（add_node 的 type 只能取它返回的值）。
- 用 add_node(type, source_anchor_id) 建节点并接上游——source_anchor_id 普通链式连接直接传上游节点 id（走它的正常输出口）；接 judge 某分支或某节点异常输出时，先 get_node_anchors(上游id) 取该出口的 source_anchor_id 传入。add_edge(source_anchor_id, target) 补「合流」额外入边，configure_node(nodeId, input) 配各节点字段（用 input 传参数：普通字段给值/引用意图，需要生成内容的字段如 code/脚本/提示词只写「要生成什么」的描述、别写真实内容，节点据描述自行生成并落库，返回 field_list 供接下游）。涉及数据库/缓存/模型的节点，其子 agent 会自行查资源。
- 处理器没有对话会话：不要使用 context-query-node / context-save-node。整条流程最终要有 response-node 把结果返回调用方（若本步就是收尾步，负责把 response-node 建好接好）。
- 只做本步要求的事，不要改动其它步骤已建好的节点。做完用一段话总结本步结果即结束。`

/**
 * 分步委派工具（处理器画布版）：把计划中的一步交给一个 worker 子 agent，在当前画布上搭建。
 * worker 只拿建图基元（无 plan / 无 execute_step），故只嵌一层、不会无限递归；
 * 子 agent 跑在同一 lf、position 追加 "step:序号"，返回 Executor 顺序 await（同画布不能并发改图）。
 */
function executeStepTool(b: AgentBindings): AgentTool {
  let seq = 0
  return {
    name: 'execute_step',
    description:
      '执行计划中的一步：委派子 agent 在当前处理器画布上把这一步涉及的节点建好、连好、配好（子过程作为可展开子任务显示）。',
    parameters: {
      type: 'object',
      properties: {
        title: {
          type: 'string',
          description: '这一步的简短标题（对应 plan 里的步骤，便于用户跟踪）'
        },
        requirement: {
          type: 'string',
          description: '这一步要做什么（自然语言）：建哪些节点、怎么连、引用哪些上游输出'
        }
      },
      required: ['requirement']
    },
    apply: (args) => {
      seq += 1
      const child: AgentBindings = {
        ...b,
        position: [...(b.position ?? []), `step:${seq}`],
        // worker 以父至此的对话作初始历史：看清整体计划与此前各步已做什么，再干本步
        messages: inheritMessages(b)
      }
      const agent = createAgent(
        useAgentConfig(
          {
            prompt: WORKER_PROMPT,
            tools: [
              getCanvasDetailTool(child),
              listNodesTool(child),
              getNodeAnchorsTool(child),
              addNodeTool(child),
              addEdgeTool(child),
              configureNodeTool(child),
              validateTool(child)
            ]
          },
          child
        )
      )
      return { promise: agent.run(args.requirement), cancel: agent.stop }
    }
  }
}

/** 处理器画布父 agent 的装配工厂：规划 + 建图基元 + 分步委派 */
export function useAgent(bindings: AgentBindings): RunnableAgent {
  return useAgentConfig(
    {
      prompt: PROMPT,
      tools: [
        planTool(bindings),
        getCanvasDetailTool(bindings),
        listNodesTool(bindings),
        getNodeAnchorsTool(bindings),
        addNodeTool(bindings),
        addEdgeTool(bindings),
        configureNodeTool(bindings),
        validateTool(bindings),
        executeStepTool(bindings)
      ]
    },
    bindings
  )
}
