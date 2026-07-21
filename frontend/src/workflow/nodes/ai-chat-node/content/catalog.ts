import { cloneDeep } from 'lodash'
import { aiChatNode } from '@/workflow/common/data'
import { PROMPT_VARIABLE_DOC, type NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { AGENT_TOOL_DEFINITIONS, AGENT_TOOL_NAMES } from '@/workflow/ai-generate/agent-tools'

/** AI 生成目录条目（结构以 content/index.vue 与后端 AIChat 为准） */
export const catalog: NodeCatalogDef = {
  entry: {
    type: 'ai-chat-node',
    label: 'AI 对话',
    summary: '调用大语言模型生成回答，回答实时流式展示给用户',
    inputs: [
      { key: 'modelId', label: '模型', type: 'string', required: true, description: '模型资源 id，先调用 get_models 获取' },
      { key: 'system', label: '系统提示词', type: 'string', default: '', description: '支持变量指令（见变量引用说明）' },
      { key: 'user', label: '用户提示词', type: 'string', default: '', required: 'enableContext=false 时', description: '支持变量指令' },
      { key: 'enableContext', label: '自定义上下文', type: 'boolean', default: false, description: 'true 时使用 contextVariable 指定的消息数组作为上下文；false 时用对话历史' },
      { key: 'contextVariable', label: '上下文变量', type: 'array', reference: true, required: 'enableContext=true 时', description: '消息数组来源，如 [外层循环ID,"compress_context"]' },
      { key: 'contextNumber', label: '上下文轮次', type: 'number', default: 0, description: '限制历史轮次，0=全部' },
      { key: 'images', label: '图片引用', type: 'array', default: [], description: '元素为 [节点ID,字段]，一般留空' },
      { key: 'videos', label: '视频引用', type: 'array', default: [], description: '元素为 [节点ID,字段]，一般留空' },
      {
        key: 'tools',
        label: '工具配置',
        type: 'object',
        default: { location: 'customize', reference: [], tools: [] },
        description:
          'agent 场景给模型定义可调用函数：{ location:"customize", reference:[], tools:[...] }。' +
          '★ 使用系统预置标准工具时每项只写 {"type":"function","function":{"name":"标准名"}}，系统自动替换为完整标准定义。' +
          `标准名（禁止改名/自编参数）：${AGENT_TOOL_NAMES.join(', ')}。` +
          '仅业务自定义函数（不由工具节点执行）才需要完整写 name/description/parameters'
      }
    ],
    outputs: [
      { value: 'content', label: '回答结果', type: 'string' },
      { value: 'reasoningContent', label: '思考过程', type: 'string' },
      { value: 'refusal', label: '拒绝原因文本', type: 'string' },
      { value: 'isRefusal', label: '是否拒绝回答', type: 'boolean' },
      {
        value: 'toolCalls',
        label: '工具调用',
        type: 'array',
        description: '元素 { id, functionName 函数名, functionArguments 参数JSON字符串, type }；agent 场景用 foreach 循环遍历它'
      },
      { value: 'finishReason', label: '结束原因', type: 'string', description: '"stop" 正常结束 / "tool_calls" 模型发起了工具调用' }
    ],
    notes: [PROMPT_VARIABLE_DOC],
    template: aiChatNode,
    normalizeNodeData: (nodeData) => {
      // 标准工具函数定义以内置模板为准整体替换（后端工具节点按固定函数名/参数解析，防止模型自编导致错乱）
      const tools = nodeData.tools?.tools
      if (Array.isArray(tools)) {
        nodeData.tools.tools = tools.map((tool: any) => {
          const canonical = AGENT_TOOL_DEFINITIONS[tool?.function?.name]
          return canonical ? cloneDeep(canonical) : tool
        })
      }
      return nodeData
    }
  }
}
