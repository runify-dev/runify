import { type Node, type Tree } from '@/api/type/node'
import { cloneDeep } from 'lodash'
import { type DataTableFilterMeta } from 'primevue/datatable'
import Clipboard from 'vue-clipboard3'
export const toTree = (nodeList: Array<Tree>) => {
  nodeList = cloneDeep(nodeList)
  const nodeMap = Object.fromEntries(nodeList.map((item) => [item.id, item]))
  const childrenList = new Set<string>()
  for (let index = 0; index < nodeList.length; index++) {
    const element = nodeList[index]
    if (!element.children) {
      element.children = []
    }
    if (element.parentId) {
      const pNode = nodeMap[element.parentId]
      if (pNode) {
        if (!pNode.children) {
          pNode.children = []
        }
        childrenList.add(element.id)
        pNode.children.push(element)
      }
    }
  }
  return nodeList.filter((item) => !childrenList.has(item.id))
}

export const generateAnchor = (
  id: string,
  direction: 'left' | 'right' | 'top' | 'bottom',
  branch: 'main' | string,
  status: 'success' | 'fail'
) => {
  return `${id}_${direction}_${branch}_${status}`
}

export const groupBy = (groupArray: Array<any>, key: string | ((item: any) => string)) => {
  return groupArray.reduce((acc: any, item: any) => {
    const _key = typeof key == 'string' ? item[key] : key(item)
    if (!acc[_key]) {
      acc[_key] = []
    }
    // 将当前项添加到对应的分类数组中
    acc[_key].push(item)
    return acc
  }, {})
}

export const formatDateTime = (date = new Date()) => {
  const pad = (n: any) => n.toString().padStart(2, '0')

  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const seconds = pad(date.getSeconds())

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}
export const getActiveFilters = (f: DataTableFilterMeta) => {
  const result: Record<string, any> = {}

  Object.keys(f).forEach((key) => {
    const field = f[key]

    if (!field || typeof field === 'string') return // 如果是空值或 string，跳过

    // 单值过滤
    if ('value' in field) {
      if (field.value != null) result[key] = field.value
    }
    // 复合过滤
    else if ('operator' in field && Array.isArray(field.constraints)) {
      const val = field.constraints.map((c) => c.value).filter((v) => v != null)
      if (val.length > 0) result[key] = val.length === 1 ? val[0] : val
    }
  })

  return result
}
export const resetUrl = (url: string, defaultUrl?: string) => {
  if (url && url.startsWith('./')) {
    return `${window.RUNIFY_APP.baseURL}/${url.substring(2)}`
  }
  return url ? url : defaultUrl ? defaultUrl : ''
}

export async function copyContent(content: string) {
  const { toClipboard } = Clipboard()
  try {
    return await toClipboard(content)
  } catch (e) {
    console.error(e)
  }
}
