export default {
  entry: 'AI 生成工作流',
  title: 'AI 生成工作流',
  modelLabel: '模型',
  modelPlaceholder: '请选择模型',
  requirementLabel: '需求描述',
  requirementPlaceholder: '描述你想要的工作流，例如：做一个基于知识库的问答应用',
  doneToast: '生成完成，请检查后保存',
  followUpPlaceholder: '继续沟通：哪里需要调整，直接说…（Enter 发送）',
  status: {
    paused: '已暂停',
    running: '生成中…'
  },
  action: {
    generate: '开始生成',
    pause: '暂停',
    resume: '继续',
    stop: '停止',
    retry: '重试',
    restart: '重新开始',
    expand: '展开日志',
    collapse: '收起',
    send: '发送'
  },
  log: {
    started: '开始生成工作流…',
    done: '生成完成',
    paused: '已暂停，点击「继续」恢复生成',
    resumed: '继续生成…',
    stopped: '已停止，画布保留当前进度',
    retrying: '重试本轮…',
    maxIterations: '已达到本轮次数上限，自动暂停；点击「继续」接着生成'
  },
  tool: {
    clear_workflow: '清空画布',
    plan: '更新规划',
    get_node_schema: '查询节点文档',
    add_node: '添加节点',
    update_node: '更新节点',
    add_edge: '连线',
    delete_edge: '删除连线',
    delete_node: '删除节点',
    get_workflow: '读取画布',
    get_node_detail: '读取节点配置',
    get_models: '获取模型列表',
    get_knowledge_bases: '获取知识库列表',
    get_database_pools: '获取数据库连接列表',
    get_database_tables: '获取数据表列表',
    get_database_columns: '获取表字段结构',
    validate_workflow: '校验工作流',
    locate_node: '定位节点',
    finish: '完成'
  }
}
