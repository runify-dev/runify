import { useAgentConfig, listNodeCatalog } from '../../common'
import { createAgent } from '../../create-agent'
import {
  nodeSignatureFor,
  getCanvasDetailTool,
  getNodeAnchorsTool,
  addNodeTool,
  addEdgeTool,
  configureNodeTool,
  validateTool,
  toLoopVariant,
  inheritMessages
} from '../../tools'
import { listTemplates, describeTemplate } from './template'
import type { AgentBindings, AgentTool, RunnableAgent, FieldShape } from '../../type'

/**
 * 对话应用画布（APPLICATION）的父 agent：一个 agent 照参考范式 + 节点签名，亲手把流程建出来。
 * 节点签名与参考范式在装配期就由 buildPrompt 内联进系统提示词，模型直接照着建。
 * 字段由 configure_node 委派各节点子 agent 落地；循环体也由本 agent 亲手建——建图工具带 lf_id 跨画布操作
 * （主画布 "main" / 循环体 = 该循环节点 id），不再起循环体构建子 agent。
 */

/** 把一个字段结构渲染成紧凑签名：value:type(枚举)[数组元素]{对象字段}，可递归 */
function renderShape(f: FieldShape): string {
  let s = `${f.value}:${f.type}`
  if (f.enum?.length) s += `(${f.enum.join('|')})`
  if (f.items?.length) s += `[{${f.items.map(renderShape).join(', ')}}]`
  if (f.fields?.length) s += `{${f.fields.map(renderShape).join(', ')}}`
  return s
}

/** 渲染某节点类型的一行签名：- type（name） in[入参] out[出参] */
function renderNodeLine(type: string, name: string): string {
  const sig = nodeSignatureFor(type)
  const props = sig?.input?.properties ?? {}
  const inParts = Object.entries(props).map(([k, v]: [string, any]) => {
    let s = `${k}:${v?.type ?? 'any'}`
    if (Array.isArray(v?.enum) && v.enum.length) s += `(${v.enum.join('|')})`
    return s
  })
  const outParts = (sig?.outputs ?? []).map(renderShape)
  return `- ${type}（${name}） in[${inParts.join(', ') || '无'}] out[${outParts.join(', ') || '无'}]`
}

/** 内联「可用节点（含签名）」：主画布目录 + 循环体变体里多出来的节点（loop-break/continue 等） */
function renderCatalog(workflowType: string): string {
  const main = listNodeCatalog(workflowType)
  const mainTypes = new Set(main.flatMap((g) => g.nodes.map((n) => n.type)))
  const sections = main.map(
    (g) => `【${g.group}】\n${g.nodes.map((n) => renderNodeLine(n.type, n.name)).join('\n')}`
  )
  const loopOnly = listNodeCatalog(toLoopVariant(workflowType))
    .flatMap((g) => g.nodes)
    .filter((n) => !mainTypes.has(n.type))
  if (loopOnly.length) {
    sections.push(
      `【循环体内才有的节点（如 loop-break/continue）——建这些要带 lf_id=所在循环节点 id 建进循环体，绝不建在主画布】\n${loopOnly
        .map((n) => renderNodeLine(n.type, n.name))
        .join('\n')}`
    )
  }
  return sections.join('\n\n')
}

/** 内联「参考范式」：每个范式的 when + recipe + blueprint 结构树 */
function renderTemplates(): string {
  return listTemplates()
    .map((t) => {
      const d = describeTemplate(t.name)
      if (!d || 'error' in d) return `# ${t.name}（${t.when}）`
      const bp = d.blueprint ? `\nblueprint（结构参考，loop 用 children、judge 用 branches）:\n${JSON.stringify(d.blueprint)}` : ''
      return `# ${d.name}（${d.when}）\nrecipe:\n${d.recipe}${bp}`
    })
    .join('\n\n')
}

/** 建图操作说明（静态部分）；节点目录/参考范式由 buildPrompt 动态拼在后面 */
const GUIDE = `你是「对话应用工作流」的搭建助手，在一张 LogicFlow 画布上按用户需求，用建图工具亲手把流程搭出来（包括循环体——你一个 agent 就能跨画布建，用 lf_id 指定往哪张画布建）。下方已内联全部「可用节点（含函数签名）」与「参考范式」，照着建即可，无需再查询节点或范式。

约定：
- 画布上始终存在开始节点，id 固定 "start-node"，不可删除/配置；输出 question（含 content/images/texts/videos/files）与 messages，下游按 [start-node,字段] 引用。
- 画布用 lf_id 标识：主画布 = "main"（或省略 lf_id）；某个循环的「循环体」= 那个循环节点的 id。建图工具（add_node/add_edge/configure_node/get_node_anchors）都收 lf_id——往哪张画布操作就传哪张的 lf_id。每个循环体里恒有一个固定节点 loop-start-node，循环体第一个节点用 add_node 接到它上面。

【怎么搭（从零）】
1. 先调 plan：它据用户需求从【参考范式】选型、产出实例化蓝图 { template, blueprint, variables, notes }。blueprint 就是要建的节点树（loop 用 children 装循环体、judge 用 branches 表分支），每个节点的 intent 是它的配置意图；variables 是本流程要声明的循环/全局变量契约。之后严格照蓝图建；并保证 variables 里的变量先在对应循环节点(loopVariables)/开始节点上声明，其它节点(context-manage/ai-chat/variable-assign/context-save 等)才引用得到。（下方也内联了参考范式供你对照。）
2. 照下方【可用节点】的签名（in=配置字段、out=可被 [节点ID,字段] 引用的出参，带类型/枚举/结构）决定怎么接线。
3. 照蓝图亲手建（主画布 lf_id="main"）：
   · add_node(type, source_anchor_id) 建节点并接上游（建即接，杜绝孤立节点）；第一个节点通常接 "start-node"。普通链式连接 source_anchor_id 直接传上游节点 id；接 judge 分支/异常口时先 get_node_anchors(上游id) 取该出口的 source_anchor_id。
   · 循环体（关键）：循环也是你自己建。① 在主画布 add_node 建出循环节点、接好上游；② configure_node(它) 配好循环本身的字段（loopType、遍历数组、循环变量——它的配置 agent 只配这些，不会帮你建循环体）；③ 然后带 lf_id=该循环节点 id 往它的循环体里建：循环体第一个节点 add_node(type, source_anchor_id="loop-start-node", lf_id=该循环id)，其余节点照常在该 lf_id 下 add_node/add_edge 串起来，无限循环要退出就放 loop-break-node；④ 循环体里每个节点建好后 configure_node(nodeId, lf_id=该循环id) 配它。嵌套循环同理：内层循环节点建在外层的循环体里(lf_id=外层id)，它自己的循环体再用 lf_id=内层id 建，一路下去。
   · judge 多分支：先 configure_node 把各分支条件配好，再 get_node_anchors(judge id) 取每个分支出口，用 add_node/add_edge 把该分支下游接上；多分支汇合到同一后继用 add_edge 补入边。
   · 每个节点建好、接好边后调 configure_node(nodeId, input, lf_id) 配它——用 input 传本节点需要的参数（按 list_nodes 里该节点的 input 结构）：普通字段给值或引用意图；★两类字段只表意图别给最终值：「生成内容」的字段（js 的 code、脚本、提示词）只写要生成什么的描述；「选资源」的字段（模型 model_id、知识库 knowledgeIds、连接池 poolId/cacheId 等）别猜真实 id、至多写意图。节点自己的配置 agent 会据描述生成内容、用 get_models/get_knowledge_bases/get_database_pools 等查出真实资源 id，再落库、把引用对上游。你的重点仍是把【结构】搭对（选对节点、连对边、传对 lf_id）＋用 input 说清每个节点要干什么。返回的 field_list 就是该节点配完后可被 [nodeID,字段] 引用的真实出参，据此接下游。
4. 边界铁律：context-query / context-save / start 只属主画布(lf_id="main")、各出现一次，绝不建进任何循环体；loop-break/continue 只属循环体、绝不建在主画布。
5. 跨切面约定（照你这次蓝图的实际形态，用到才管、用不到就忽略——不同用户/任务的蓝图千差万别，别默认往某个范式套）：
   · 蓝图里有「按工具名分流的循环」（如 agent 循环的工具分发）时：ai-chat 的 tools 声明的函数名要与内层各真实工具节点一一对应，绝不自造工具名。
   · 蓝图会产文件时：放 file-upload，最终答复用 Markdown 交付下载链接。
   · 记忆闭环（agent 循环这类带记忆/多步结构）：循环变量要先在循环节点上声明(loopVariables)；context-manage 读写同一组循环变量做原地滚动；ai-chat 的 contextVariable 引这组变量；循环后的 context-save 引用它们保存——一处引用到未声明的变量就会断链，务必对齐。
6. 搭完调 validate 自检，problems 非空按提示修，直到合格。

【小改动/续聊微调】get_canvas_detail 看现状（跨循环层的结构全景，每个循环体挂在 loopBodies[循环id] 下）后，直接带对应 lf_id 用 add_node / add_edge / configure_node 就地处理——要改某个循环体内部，传 lf_id=那个循环节点 id 即可。
- 对话应用通常以 ai-chat-node 流式回答；需结构化数据响应再加 response-node。
- 无需再调用工具时，用一段话总结即结束。`

/** 装配期构建完整系统提示词：操作说明 + 内联节点签名目录 + 内联参考范式 */
function buildPrompt(bindings: AgentBindings): string {
  return `${GUIDE}

======== 可用节点（含函数签名） ========
${renderCatalog(bindings.workflowType)}

======== 参考范式 ========
${renderTemplates()}`
}

/** 规划器 system（选型 + 实例化蓝图）；参考范式与节点目录由 buildPlanPrompt 内联在后面 */
const PLAN_GUIDE = `你是「对话应用工作流」的规划器。根据用户需求，从下方【参考范式】选最合适的一种，产出一份贴合用户需求的执行蓝图，供搭建 agent 照着建。你只规划、不建节点、不改画布。

要求：
- 先按范式的 when 选型：做「能完成 X 的智能体/助手」走 agent 循环范式；一问一答/内容生成走简单范式；纯知识检索问答走 RAG；按类型分流走 routing；多步串联走 pipeline。
- 范式（含 blueprint）是【参照知识】，不是照抄或照删的固定清单：先真正理解用户这次的问题要什么，再参照范式的结构与套路，按这个需求实际需要什么就放什么，组出一份贴合它的蓝图——需求千人千面，节点与工具因任务而异（做 PPT 的 agent 和做数据分析的 agent，内层工具本就不同）。
- 实例化：定该有哪些节点、谁在谁的循环体(children)/分支(branches)里、大致顺序，并给每个关键节点写一句 intent（它在链路里要干什么：ai-chat 的身份/目标/产出、内层 loop 遍历哪个数组、judge 各分支条件、工具节点对应哪个函数名）。
- agent 循环范式：内层「按 functionName 分流的工具集」就是这个 agent 为完成用户任务实际要用的那些工具，ai-chat 的 tools 声明的函数名与内层各真实工具节点一一对应。
- 定「变量契约」：想清楚流程需要哪些【跨节点共享的变量】并在 submit_plan 的 variables 里列出（scope: "global" 或 "loop"＋这个循环在蓝图里是谁；name；dataType；defaultValue；purpose）。声明这些变量的节点（循环节点的 loopVariables、开始节点/variable-assign 的全局变量）据此声明，其它节点才引用得到——凡后续要被 context-manage/ai-chat/variable-assign/context-save 等引用的循环/全局变量，都要在这里先列出。典型：agent 记忆循环要在外层 infinite 循环上声明 context(array,[])、summary(dict,{})、facts(array,[])。
- 只用下方【可用节点】里的真实 type，别自造。
- 想清楚后调 submit_plan 交出蓝图与变量契约，交完即结束。`

/** 规划器开工指令（run 参数） */
const PLAN_TASK =
  '参照参考范式、结合用户这次的具体问题，组出一份贴合该需求的实例化蓝图（用得上的节点/工具才放），并定好变量契约（哪些循环/全局变量要声明），最后调 submit_plan 交出。'

/** 装配规划器 system：规划指引 + 内联参考范式 + 内联节点目录 */
function buildPlanPrompt(bindings: AgentBindings): string {
  return `${PLAN_GUIDE}

======== 参考范式 ========
${renderTemplates()}

======== 可用节点（含函数签名） ========
${renderCatalog(bindings.workflowType)}`
}

/**
 * 规划器终态工具：交出蓝图。返回 { status:'stop', data } —— 交卷信号：createAgent 一见 stop 即短路收尾，
 * 蓝图作为规划器 AgentResult.data 上抛给 plan 工具。交出即结束。
 */
function submitPlanTool(): AgentTool {
  return {
    name: 'submit_plan',
    description:
      '交出规划蓝图：template 选中的范式名，blueprint 实例化后的节点树，variables 变量契约，notes 关键配置意图与选型理由。这是规划最后一步，交出即结束。',
    parameters: {
      type: 'object',
      properties: {
        template: { type: 'string', description: '选中的参考范式名' },
        blueprint: {
          type: 'array',
          description:
            '实例化后的节点树：元素 { type, intent?, children?, branches? }；loop 用 children 装循环体，judge 用 branches（[{cond, children}]）表多输出',
          items: { type: 'object' }
        },
        variables: {
          type: 'array',
          description:
            '变量契约：本流程要声明的跨节点共享变量。元素 { scope:"global"|"loop", loop?:该循环在蓝图里是谁（scope=loop 时，用意图描述定位）, name, dataType:"string"|"number"|"boolean"|"array"|"dict", defaultValue, purpose }。' +
            'agent 记忆循环通常在外层 infinite 循环上含 context(array,[])、summary(dict,{})、facts(array,[])；无共享变量则空数组。',
          items: { type: 'object' }
        },
        notes: { type: 'string', description: '关键配置意图与选型理由（自然语言）' }
      },
      required: ['template', 'blueprint']
    },
    apply: (args) => ({
      status: 'stop',
      data: {
        template: typeof args.template === 'string' ? args.template : '',
        blueprint: Array.isArray(args.blueprint) ? args.blueprint : [],
        variables: Array.isArray(args.variables) ? args.variables : [],
        notes: typeof args.notes === 'string' ? args.notes : ''
      }
    })
  }
}

/**
 * 规划委派工具（对话应用）：把「选型 + 出蓝图」交给一个规划器子 agent。
 * 规划器读用户需求（context 折进其 system）+ 继承的父对话 + 内联的参考范式/节点目录，产出实例化蓝图；只规划、不改画布。
 * 返回 { template, blueprint, notes }：父 agent 据此照建；这份蓝图也随父对话继承给各层循环体构建 agent，统一大家的搭建预期。
 */
export function planTool(b: AgentBindings): AgentTool {
  return {
    name: 'plan',
    description:
      '（开工第一步）委派规划 agent：据用户需求从参考范式选型、产出实例化执行蓝图与变量契约。返回 { template, blueprint, variables, notes }——blueprint 是要建的节点树、每个节点的 intent 是配置意图；variables 是要声明的循环/全局变量。你据此照建，并确保 variables 里的变量先在对应循环节点(loopVariables)/开始节点上声明，其它节点才引用得到。',
    parameters: { type: 'object', properties: {} },
    apply: () => {
      const planner: AgentBindings = {
        ...b,
        position: [...(b.position ?? []), 'plan'],
        messages: inheritMessages(b)
      }
      const agent = createAgent(
        useAgentConfig(
          { prompt: buildPlanPrompt(b), tools: [getCanvasDetailTool(planner), submitPlanTool()] },
          planner
        )
      )
      const promise = agent.run(PLAN_TASK).then((result) => {
        // 规划器以 submit_plan(stop) 交付 → result.data = { template, blueprint, variables, notes }
        const plan = result.data
        if (result.status !== 'done' || !plan || typeof plan !== 'object' || !(plan.template || plan.blueprint?.length)) {
          const msg = typeof result.data === 'string' ? result.data : ''
          return { error: msg || '规划未产出蓝图' }
        }
        return {
          template: plan.template,
          blueprint: plan.blueprint,
          variables: plan.variables,
          notes: plan.notes
        }
      })
      return { promise, cancel: agent.stop }
    }
  }
}

/** 对话应用画布父 agent 的装配工厂：节点签名与参考范式内联进 prompt，工具＝建图基元 */
export function useAgent(bindings: AgentBindings): RunnableAgent {
  return useAgentConfig(
    {
      prompt: buildPrompt(bindings),
      tools: [
        getCanvasDetailTool(bindings),
        getNodeAnchorsTool(bindings),
        addNodeTool(bindings),
        addEdgeTool(bindings),
        configureNodeTool(bindings),
        validateTool(bindings)
      ]
    },
    bindings
  )
}
