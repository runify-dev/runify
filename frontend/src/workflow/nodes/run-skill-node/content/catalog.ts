import { runSkillNode } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'
import { triple, toolModeInputs, HIDDEN_TOOL_OUTPUT } from '@/workflow/ai-generate/catalog-library'

export const catalog: NodeCatalogDef = {
  entry: {
    type: 'run-skill-node',
    label: '技能执行',
    summary: '执行已安装技能的命令（自动注入技能环境变量）',
    inputs: [
      { key: 'runtime', label: '运行时', type: 'string', default: 'local', description: '保持 "local"' },
      ...toolModeInputs(),
      triple('skillId', '技能 id', 'string', { required: 'location="customize" 时', default: '' }),
      triple('command', '执行命令', 'string', { required: 'location="customize" 时', default: '' })
    ],
    outputs: [
      { value: 'result', label: '执行结果', type: 'string' },
      { value: 'stdout', label: '标准输出', type: 'string' },
      { value: 'stderr', label: '错误输出', type: 'string' },
      { value: 'exitCode', label: '退出码', type: 'number' },
      HIDDEN_TOOL_OUTPUT
    ],
    template: runSkillNode
  }
}
