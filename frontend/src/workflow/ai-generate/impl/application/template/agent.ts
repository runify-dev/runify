import type { WorkflowTemplate } from './types'

/**
 * 智能体（agent）：带记忆的多步自主循环，模型反复调用工具直到完成任务。
 * 这是「做一个能干 X 的智能体」类需求的通用骨架——PPT 生成、数据分析、调研、代码修改…
 * 结构都相同，变的只是「循环内 ai-chat 的 system 提示词」与「内层挂哪些工具」。
 * blueprint 为纯结构骨架（只有 type + 嵌套 + judge 分流条件，无描述），逐节点镜像真实内置模板
 * （views/application/template/agent/template.json）；任务意图不进节点，看 recipe 与用户需求。
 */
export const agent: WorkflowTemplate = {
  name: '智能体(agent)',
  when: '「做一个能完成 X 的智能体/助手」——X 可以是 PPT 生成、数据分析、调研、内容创作、代码修改、自动化等任何任务。只要需要模型多步自主决策、反复调用工具（可含知识检索），就用这套循环骨架；不同任务只改循环内提示词与工具，结构不变。这是 agent 类需求的默认范式。',
  recipe: [
    '结构照 blueprint 搭（纯骨架：loop 的 children 是循环体、judge 的 branches 是各分支下游）。blueprint 只表结构，以下是它表达不了、但必须遵守的约定：',
    '- 按任务只改两处：①外层循环内 ai-chat 的 system（写清 agent 身份/目标/产出规范，如「你是 PPT 制作助手，规划大纲并逐页生成，最终产出 pptx 文件」）②内层按 functionName 分流的工具集——★先按用户这次任务想清这个 agent 真正要用哪些工具（通常 3-6 个，因任务而异：做 PPT 大致是 create_file/apply_patch/run_command/read_file/file_upload；做调研才可能要 knowledge_search；做数据分析另有其他），只为这些工具建分支。blueprint 里给的工具分支只示意「一工具一分支、每支后接 context-push」的形状，不是要照抄的清单——用得上的才放，用不上的（本任务不检索知识库就别放 knowledge_search、不复用技能就别放 list_skills/run_skill 等）一律不放。ai-chat 的 tools 声明的函数名与内层工具节点一一对应。',
    '- 节点内部字段（loopVariables、context-manage 的 source、ai-chat 的 tools 标准定义、各工具节点 location=tool_call 引用当前 item 等）由 configure_node 委派各节点子 agent 配置，不写死。',
    '- 边界：context-query / context-save / start 属外层主画布、各出现一次，绝不能建进循环体；委派配置外层 loop 时只写循环体每轮要做什么。',
    '- 文件交付契约（凡方案含产文件能力必守）：只要放了 create-file / apply-patch / terminal / run-skill 等可能产文件的节点，就必须同时放 file-upload、并在 ai-chat 的 tools 里声明 file_upload；最终答复用 Markdown 表格集中交付文件（每行：文件 | 说明 | 下载，下载列写 file_upload 返回的 URL，原样使用不得改写）。'
  ].join('\n'),
  blueprint: [
    { type: 'context-query-node' },
    {
      type: 'loop-node',
      children: [
        {
          type: 'judge-node',
          branches: [
            { cond: 'index == 0', children: [{ type: 'variable-assign-node' }] },
            { cond: 'else', children: [] }
          ]
        },
        { type: 'context-manage-node' },
        { type: 'ai-chat-node' },
        {
          type: 'judge-node',
          branches: [
            {
              cond: 'finishReason == "tool_calls"',
              children: [
                {
                  type: 'loop-node',
                  children: [
                    { type: 'extract-node' },
                    {
                      type: 'judge-node',
                      // 工具分支仅示意形状（一工具一分支、每支后接 context-push）；实际用哪些工具由 recipe 按任务定，非照抄
                      branches: [
                        {
                          cond: 'functionName == "create_file"',
                          children: [{ type: 'create-file-node' }, { type: 'context-push-node' }]
                        },
                        {
                          cond: 'functionName == "run_command"',
                          children: [{ type: 'terminal-node' }, { type: 'context-push-node' }]
                        },
                        {
                          cond: 'functionName == "file_upload"',
                          children: [{ type: 'file-upload-node' }, { type: 'context-push-node' }]
                        },
                        { cond: 'else', children: [] }
                      ]
                    }
                  ]
                }
              ]
            },
            { cond: 'else', children: [{ type: 'loop-break-node' }] }
          ]
        }
      ]
    },
    { type: 'context-save-node' }
  ]
}
