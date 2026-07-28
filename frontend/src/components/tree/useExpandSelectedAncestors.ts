import type { Ref } from 'vue'
import { useRoute } from 'vue-router'
import type { TreeSelectionKeys } from 'primevue/tree'
import type { TreeManager } from './index'

/**
 * 刷新后展开当前选中资源（route.params.id）的所有祖先节点，避免选中项被折叠的父级隐藏。
 * 返回一个函数，在树数据加载并构建好 TreeManager 之后调用即可。
 */
export const useExpandSelectedAncestors = (
  treeManage: Ref<TreeManager | undefined>,
  expandedKeys: Ref<TreeSelectionKeys | undefined>
) => {
  const route = useRoute()
  return () => {
    const id = route.params.id as string
    if (!id || !treeManage.value) {
      return
    }
    const expanded: Record<string, boolean> = {}
    treeManage.value.getAncestorKeys(id).forEach((key) => {
      expanded[key] = true
    })
    expandedKeys.value = { ...expandedKeys.value, ...expanded }
  }
}
