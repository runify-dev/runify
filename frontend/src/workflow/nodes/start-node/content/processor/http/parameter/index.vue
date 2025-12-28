<template>
  <div class="flex w-full justify-between content-center items-center">
    <h5 class="break-all lighter">
      参数设置
      <span style="color: var(--el-color-danger)">*</span>
    </h5>
    <span class="ml-4" style="margin-top: -4px">
      <el-button link type="primary" @click="open()">
        <el-icon><Plus /></el-icon>
      </el-button>
    </span>
  </div>
  <el-table
    :data="tableData"
    ref="tableRef"
    row-key="field"
    class="border-l border-r"
    style="width: 100%"
  >
    <el-table-column prop="field" label="参数" width="90">
      <template #default="{ row }">
        <span :title="row.field" class="ellipsis-1">{{ row.field }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="label" label="描述">
      <template #default="{ row }">
        <span :title="row.label" class="ellipsis-1">
          {{ row.description }}
        </span>
      </template>
    </el-table-column>
    <el-table-column label="操作" height="200">
      <template #default="{ row }">
        <div class="flex justify-start">
          <el-tooltip effect="dark" content="修改" placement="top">
            <el-button class="p-0" type="primary" text @click.stop="open(row)">
              <el-icon><EditPen /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip effect="dark" content="删除" placement="top">
            <el-button class="p-0 m-0" type="primary" text @click="deleteParameter(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </template>
    </el-table-column>
  </el-table>
  <CreateParameter ref="createParameterRef" @submit="submit"></CreateParameter>
</template>
<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref, computed, inject, onMounted } from 'vue'
import CreateParameter from './CreateParameter.vue'
const createParameterRef = ref<InstanceType<typeof CreateParameter>>()
const props = defineProps<{ parameters: Array<any> }>()
const emit = defineEmits(['update:parameters'])
import type { BaseNodeModel } from '@logicflow/core'
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const tableData = computed({
  get: () => {
    return props.parameters
  },
  set: (event: any) => {
    emit('update:parameters', event)
  }
})
const open = (row?: any) => {
  createParameterRef.value?.open(row)
}
const submit = (event: any) => {
  if (event.edit) {
    for (const index in tableData.value) {
      if (tableData.value[index].field == event.row.field) {
        tableData.value[index] = event.row
        break
      }
    }
  } else {
    if (tableData.value.some((row: any) => row.field == event.row.field)) {
      ElMessage.warning('字段已存在')
      return
    }
    tableData.value.push(event.row)
  }
  createParameterRef.value?.close()

  updateFieldList()
}

const deleteParameter = (row: any) => {
  model.updateFieldList(
    tableData.value.map((item) => ({ label: item.description, value: item.field }))
  )
  tableData.value = tableData.value.filter((item: any) => item.field != row.field)
}
const updateFieldList = () => {
  model.properties.field_list = tableData.value.map((item) => ({
    label: item.description,
    value: item.field
  }))
}
onMounted(() => {
  updateFieldList()
})
defineExpose({ updateFieldList })
</script>
<style lang="scss" scoped></style>
