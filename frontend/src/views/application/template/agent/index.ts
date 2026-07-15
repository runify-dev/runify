import template from "./template.json"
import _ from 'lodash';

export const getWorkflow = (modelId: string) => {
  const newData = JSON.parse(JSON.stringify(template));
  _.set(newData, 'nodes[1].properties.nodeData.children.nodes[1].properties.nodeData.modelId', modelId);
  _.set(newData, 'nodes[1].properties.nodeData.children.nodes[5].properties.nodeData.summarizerModelId', modelId);
  return newData
}
