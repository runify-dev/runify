<template>
  <div>
    <div class="p-mb-3">
      <Button variant="text" @click="open()">添加</Button>
    </div>
    <!-- 使用树形表格展示嵌套结构 -->
    <TreeTable :value="treeTableData">
      <Column field="field" header="参数" expander>
        <template #body="slotProps">
          <div class="field-cell">
            <span>{{ slotProps.node.data.field }}</span>
          </div>
        </template>
      </Column>

      <Column field="description" header="描述"></Column>

      <Column header="操作" style="width: 150px">
        <template #body="slotProps">
          <div class="action-buttons">
            <Button
              v-if="slotProps.node.data.type === 'object'"
              icon="pi pi-plus"
              variant="text"
              rounded
              size="small"
              @click.stop="open(null, undefined, slotProps.node.data)"
              title="添加子字段"
            />
            <Button
              icon="pi pi-file-edit"
              variant="text"
              rounded
              size="small"
              @click.stop="open(slotProps.node.data, slotProps.node.key)"
            />
            <Button
              icon="pi pi-times-circle"
              variant="text"
              rounded
              size="small"
              @click="deleteParameter(slotProps.node.data, slotProps.node.key)"
            />
          </div>
        </template>
      </Column>
    </TreeTable>

    <CreateParameter ref="createParameterRef" :parent-field="currentParentField" @submit="submit" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import CreateParameter from './CreateBody.vue'
import TreeTable from 'primevue/treetable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import bus from '@/bus'
const createParameterRef = ref<InstanceType<typeof CreateParameter>>()
const currentParentField = ref<any>(null)

const props = defineProps<{
  body: Array<any>
  updateFieldList: () => void
}>()

const emit = defineEmits(['update:body'])

// 将扁平数据转换为树形结构
const treeTableData = computed(() => {
  return convertToTreeNodes(props.body || [])
})

// 转换函数
function convertToTreeNodes(fields: any[], parentKey: string = 'root'): any[] {
  return fields.map((field, index) => {
    const key = parentKey ? `${parentKey}_${field.field}_${index}` : `${field.field}_${index}`
    const node = {
      key,
      data: field,
      children: field.children ? convertToTreeNodes(field.children, key) : null
    }
    return node
  })
}

// 查找节点路径
function findNodePath(key: string, nodes: any[]): any[] | null {
  for (const node of nodes) {
    if (node.key === key) {
      return [node]
    }
    if (node.children) {
      const path = findNodePath(key, node.children)
      if (path) {
        return [node, ...path]
      }
    }
  }
  return null
}

// 根据路径获取父节点数据
function getParentDataByPath(path: any[]): any {
  if (path.length <= 1) return null
  return path[path.length - 2]?.data
}

// 打开创建/编辑对话框
const open = (row?: any, key?: string, parentField?: any) => {
  if (parentField) {
    currentParentField.value = parentField
  } else if (key) {
    // 如果是编辑，找到父节点
    const path = findNodePath(key, treeTableData.value)
    if (path && path.length > 1) {
      currentParentField.value = path[path.length - 2]?.data
    } else {
      currentParentField.value = null
    }
  } else {
    currentParentField.value = null
  }

  createParameterRef.value?.open(row, key)
}

// 提交表单
const submit = (event: any) => {
  if (event.edit) {
    // 编辑现有字段
    const updatedTreeData = updateFieldByPath(event.key, event.row, treeTableData.value)
    emit('update:body', convertToOriginalFields(updatedTreeData))
  } else {
    // 新增字段
    if (currentParentField.value) {
      // 添加子字段
      if (!currentParentField.value.children) {
        currentParentField.value.children = []
      }
      if (currentParentField.value.children.some((child: any) => child.field === event.row.field)) {
        bus.emit('message:warn', '子字段已存在')
        return
      }
      currentParentField.value.children.push(event.row)
    } else {
      // 添加顶级字段
      if (props.body?.some((row: any) => row.field === event.row.field)) {
        bus.emit('message:warn', '字段已存在')
        return
      }
      let newData: any = []
      if (!props.body) {
        newData = []
      } else {
        newData = [...props.body, event.row]
      }
      emit('update:body', newData)
    }
  }

  createParameterRef.value?.close()
  props.updateFieldList()
}

// 根据路径更新字段 - 返回新数组而不是直接修改
function updateFieldByPath(key: string, newData: any, nodes: any[]): any[] {
  return nodes.map((node) => {
    // 找到要更新的节点
    if (node.key === key) {
      // 保留原有的 children
      const updatedNode = {
        ...node,
        data: {
          ...newData,
          children: node.data.children // 保留原有的子节点
        }
      }
      return updatedNode
    }

    // 如果有子节点，递归处理
    if (node.children && node.children.length > 0) {
      const updatedChildren = updateFieldByPath(key, newData, node.children)

      // 只有当子节点有变化时才创建新对象
      if (updatedChildren !== node.children) {
        return {
          ...node,
          children: updatedChildren
        }
      }
    }

    return node
  })
}
// 删除参数
const deleteParameter = (row: any, key: string) => {
  // 创建树的副本，避免直接修改 props.body
  const updatedTreeData = removeFieldByPath(key, treeTableData.value)
  emit('update:body', convertToOriginalFields(updatedTreeData)) // 更新父组件的 body 数据
  props.updateFieldList() // 刷新字段列表
}
function removeFieldByPath(key: string, nodes: any[]): any[] {
  return nodes
    .map((node) => {
      if (node.key === key) {
        return null // Found node to delete
      }

      if (node.children && node.children.length > 0) {
        const updatedChildren = removeFieldByPath(key, node.children)
        // After recursion, ensure we're not passing empty arrays
        return updatedChildren.length > 0
          ? { ...node, children: updatedChildren }
          : { ...node, children: [] }
      }

      return node
    })
    .filter(Boolean) // Remove nulls (deleted nodes)
}
// 将树形结构转换为扁平结构
function convertToOriginalFields(treeNodes: any[]): any[] {
  return treeNodes.map((node) => {
    const fieldData = { ...node.data }

    // 如果有子节点，递归转换并添加到children属性
    if (node.children && node.children.length > 0) {
      fieldData.children = convertToOriginalFields(node.children)
    } else {
      // 如果没有子节点，确保children属性被移除或设置为undefined
      delete fieldData.children
    }

    return fieldData
  })
}

// 监听参数变化，更新树形数据
watch(
  () => props.body,
  () => {
    // 强制更新树形数据
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>
.field-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.default-value {
  color: #10b981;
  font-family: monospace;
  background: #f0fdf4;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9rem;
}

:deep(.p-treetable) {
  margin-top: 1rem;
  border: 1px solid #e9ecef;
  border-radius: 6px;
}

:deep(.p-treetable .p-treetable-tbody > tr > td) {
  padding: 0.75rem 1rem;
}
</style>
