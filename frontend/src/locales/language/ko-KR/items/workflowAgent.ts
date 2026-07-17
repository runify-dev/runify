export default {
  entry: 'AI 워크플로 생성',
  title: 'AI 워크플로 생성',
  modelLabel: '모델',
  modelPlaceholder: '모델을 선택하세요',
  requirementLabel: '요구 사항',
  requirementPlaceholder: '원하는 워크플로를 설명하세요. 예: 지식 베이스 기반 Q&A 앱',
  doneToast: '생성이 완료되었습니다. 확인 후 저장하세요',
  followUpPlaceholder: '계속 소통: 조정할 부분을 말해 주세요… (Enter 전송)',
  status: {
    paused: '일시 정지됨',
    running: '생성 중…'
  },
  action: {
    generate: '생성 시작',
    pause: '일시 정지',
    resume: '계속',
    stop: '중지',
    retry: '재시도',
    restart: '다시 시작',
    expand: '로그 펼치기',
    collapse: '접기',
    send: '전송'
  },
  log: {
    started: '워크플로 생성 중…',
    done: '생성 완료',
    paused: '일시 정지됨. "계속"을 눌러 재개하세요',
    resumed: '재개 중…',
    stopped: '중지되었습니다. 캔버스는 현재 상태로 유지됩니다',
    retrying: '재시도 중…',
    maxIterations: '반복 한도에 도달해 자동 일시 정지되었습니다. "계속"을 눌러 이어가세요'
  },
  tool: {
    clear_workflow: '캔버스 초기화',
    plan: '계획 업데이트',
    get_node_schema: '노드 문서 조회',
    add_node: '노드 추가',
    update_node: '노드 수정',
    add_edge: '연결',
    delete_edge: '연결 삭제',
    delete_node: '노드 삭제',
    get_workflow: '캔버스 읽기',
    get_node_detail: '노드 설정 읽기',
    get_models: '모델 목록 조회',
    get_knowledge_bases: '지식 베이스 목록 조회',
    validate_workflow: '워크플로 검증',
    locate_node: '노드로 이동',
    finish: '완료'
  }
}
