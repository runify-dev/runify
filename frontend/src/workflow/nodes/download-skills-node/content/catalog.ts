import { downloadSkillsNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'download-skills-node',
    label: '技能下载',
    summary: '按技能 id 下载安装技能',
    inputs: [...toolModeInputs(), triple('skillId', '技能 id', 'string', { required: 'location="customize" 时', default: '' })],
    outputs: [
      { value: 'skillId', label: '技能ID', type: 'string' },
      { value: 'skillName', label: '技能名称', type: 'string' },
      { value: 'files', label: '文件数', type: 'number' },
      { value: 'status', label: '安装状态', type: 'string' },
      { value: 'local', label: '本地路径', type: 'string' },
      { value: 'content', label: '技能内容', type: 'string' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: downloadSkillsNode
  }
}
