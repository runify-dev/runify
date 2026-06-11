import template from "./template.json"
import _ from 'lodash';

export const getWorkflow = (modelId: string, knowledgeIds: string[] = []) => {
  const newData = JSON.parse(JSON.stringify(template));
  _.set(newData, 'nodes[1].properties.nodeData.children.nodes[5].properties.nodeData.modelId', modelId);
  if (knowledgeIds.length > 0) {
    _.set(newData, 'nodes[1].properties.nodeData.children.nodes[7].properties.nodeData.children.nodes[3].properties.nodeData.knowledgeIds', knowledgeIds);
  }
  return newData
}
