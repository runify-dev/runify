export default {
  entry: 'AI Generate Workflow',
  title: 'AI Generate Workflow',
  modelLabel: 'Model',
  modelPlaceholder: 'Select a model',
  requirementLabel: 'Requirement',
  requirementPlaceholder:
    'Describe the workflow you want, e.g. a knowledge-base powered Q&A application',
  doneToast: 'Generation finished, please review and save',
  followUpPlaceholder: 'Keep chatting: tell me what to adjust… (Enter to send)',
  status: {
    paused: 'Paused',
    running: 'Generating…'
  },
  action: {
    generate: 'Generate',
    pause: 'Pause',
    resume: 'Resume',
    stop: 'Stop',
    retry: 'Retry',
    restart: 'Restart',
    expand: 'Expand logs',
    collapse: 'Collapse',
    send: 'Send'
  },
  log: {
    started: 'Generating workflow…',
    done: 'Generation finished',
    paused: 'Paused. Click "Resume" to continue',
    resumed: 'Resuming…',
    stopped: 'Stopped. Canvas keeps current progress',
    retrying: 'Retrying…',
    maxIterations: 'Iteration budget reached, auto paused. Click "Resume" to continue'
  },
  tool: {
    clear_workflow: 'Clear canvas',
    plan: 'Update plan',
    get_node_schema: 'Get node schema',
    add_node: 'Add node',
    update_node: 'Update node',
    add_edge: 'Connect nodes',
    delete_edge: 'Delete edge',
    delete_node: 'Delete node',
    get_workflow: 'Read canvas',
    get_node_detail: 'Read node config',
    get_models: 'List models',
    get_knowledge_bases: 'List knowledge bases',
    validate_workflow: 'Validate workflow',
    locate_node: 'Locate node',
    finish: 'Finish'
  }
}
