import {getWorkflow as agentWorkflow} from "./agent";
import {getWorkflow as customizeWorkflow} from "./customize";
import {getWorkflow as searchWorkflow} from "./search";

const kv = {
  'agent': agentWorkflow,
  'customize': customizeWorkflow,
  'search': searchWorkflow
}
export const getWorkflowCall = (type: 'agent' | 'customize' | 'search'): any => {
  return kv[type]
}
