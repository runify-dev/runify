import { listSkillsNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'list-skills-node',
    label: '技能列表',
    summary: '列出可用技能',
    inputs: [...toolModeInputs()],
    outputs: [
      {
        value: 'skills',
        label: '技能列表',
        type: 'array',
        description: '元素 { id, name, description, installed 是否已安装, hasUpdate, local 本地路径, parameters }'
      },
      { value: 'summary', label: '摘要', type: 'string' },
      { value: 'skills_count', label: '技能数', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: listSkillsNode
  }
}
