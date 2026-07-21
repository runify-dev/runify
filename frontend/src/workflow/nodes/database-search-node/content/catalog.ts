import { databaseSearchNode, WorkflowType } from '@/workflow/common/data'
import type { NodeCatalogDef } from '@/workflow/ai-generate/node-catalog'

const DB_PARAMETER_DOC =
  'SQL 参数数组。元素 { field 参数名(对应 SQL 模板中的 #{field} 占位符), location:"reference"|"customize", ' +
  'value: location=reference 时为变量引用路径 [节点ID,字段]、customize 时为常量值, desc 可选说明 }'

/** SQL 方言提示（mysql / postgresql 语法差异；参数占位符 #{名} 由后端统一处理，与方言无关） */
const SQL_DIALECT_DOC =
  'SQL 方言必须匹配连接池的数据库类型（get_database_pools 返回的 provider）：mysql 与 postgresql 语法不同——' +
  '标识符引号 MySQL 用反引号 `col`、PostgreSQL 用双引号 "col"（未加引号时 PostgreSQL 会转小写，注意大小写）；' +
  '字符串拼接 MySQL 用 CONCAT()、PostgreSQL 用 || 或 CONCAT()；模糊匹配大小写不敏感 MySQL 默认不敏感、PostgreSQL 用 ILIKE；' +
  '布尔值 PostgreSQL 支持 true/false、MySQL 用 1/0。写模板前先据 provider 选对方言。'

/** 后端仅支持 PROCESSOR_HTTP（主画布，不能进循环子画布） */
export const catalog: NodeCatalogDef = {
  workflowTypes: [WorkflowType.PROCESSOR],
  entry: {
    type: 'database-search-node',
    label: '数据库检索',
    summary: '在数据库连接池上执行查询 SQL，返回行列表',
    inputs: [
      { key: 'poolId', label: '数据库连接池', type: 'string', required: true, description: '连接池资源 id，先调用 get_database_pools 获取' },
      {
        key: 'location',
        label: 'SQL 来源',
        type: 'string',
        enum: ['customize', 'reference'],
        default: 'customize',
        description: 'customize 用 template 里的 SQL 模板；reference 引用上游变量作为完整 SQL 文本'
      },
      { key: 'reference', label: 'SQL 引用', type: 'array', reference: true, required: 'location="reference" 时' },
      {
        key: 'template',
        label: 'SQL 模板',
        type: 'string',
        default: '',
        required: 'location="customize" 时',
        description:
          '参数占位符用 #{参数名}，如 SELECT * FROM users WHERE id = #{userId}；禁止字符串拼接变量（SQL 注入）。' +
          SQL_DIALECT_DOC
      },
      { key: 'parameters', label: 'SQL 参数', type: 'array', default: [], description: DB_PARAMETER_DOC }
    ],
    outputs: [{ value: 'result', label: '查询结果', type: 'array', description: '行对象数组，元素 { 列名: 值 }' }],
    notes: ['仅处理器主画布可用，不能放进循环子画布', SQL_DIALECT_DOC],
    template: databaseSearchNode
  }
}
