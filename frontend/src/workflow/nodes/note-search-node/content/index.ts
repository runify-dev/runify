import {validate} from './validator'

export function init({model}: { model: any }) {
  if (!model.properties.nodeData) {
    model.properties.nodeData = {
      folderIds: [],
      keywordLocation: 'customize',
      keywordReference: [],
      keyword: '',
      pageNoLocation: 'customize',
      pageNoReference: [],
      pageNo: 1,
      pageSizeLocation: 'customize',
      pageSizeReference: [],
      pageSize: 10
    }
  }
  model.properties.field_list = [
    {
      label: '工具执行',
      value: 'tool'
    },
    {label: '结果列表', value: 'hits'},
    {label: '总数', value: 'total'},
    {label: '最高分', value: 'topScore'}
  ]
}

export {validate}
