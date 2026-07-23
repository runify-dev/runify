export default {
  entry: 'AI ワークフロー生成',
  title: 'AI ワークフロー生成',
  modelLabel: 'モデル',
  modelPlaceholder: 'モデルを選択してください',
  requirementLabel: '要件',
  requirementPlaceholder: '作りたいワークフローを記述してください。例：ナレッジベースを使った Q&A アプリ',
  doneToast: '生成が完了しました。確認して保存してください',
  followUpPlaceholder: '会話を続ける：調整したい点を入力…（Enter で送信）',
  status: {
    paused: '一時停止中',
    running: '生成中…'
  },
  action: {
    generate: '生成開始',
    pause: '一時停止',
    resume: '再開',
    stop: '停止',
    retry: '再試行',
    restart: '最初から',
    expand: 'ログを展開',
    collapse: '折りたたむ',
    send: '送信'
  },
  log: {
    started: 'ワークフローを生成中…',
    done: '生成完了',
    paused: '一時停止しました。「再開」で続行します',
    resumed: '再開中…',
    stopped: '停止しました。キャンバスは現状のまま保持されます',
    retrying: '再試行中…',
    maxIterations: '反復回数の上限に達し自動的に一時停止しました。「再開」で続行できます'
  },
  tool: {
    clear_workflow: 'キャンバスをクリア',
    plan: '計画を更新',
    get_node_schema: 'ノード仕様を取得',
    add_node: 'ノードを追加',
    update_node: 'ノードを更新',
    add_edge: '接続',
    delete_edge: '接続を削除',
    delete_node: 'ノードを削除',
    get_workflow: 'キャンバスを読み取り',
    get_node_detail: 'ノード設定を読み取り',
    get_models: 'モデル一覧を取得',
    get_knowledge_bases: 'ナレッジベース一覧を取得',
    get_database_pools: 'データベース接続一覧を取得',
    get_database_tables: 'テーブル一覧を取得',
    get_database_columns: 'カラム構造を取得',
    validate_workflow: 'ワークフローを検証',
    locate_node: 'ノードへ移動',
    finish: '完了'
  }
}
