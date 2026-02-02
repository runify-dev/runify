<template>
  <Fieldset legend="参数设置">
    <Button variant="text" @click="open()">添加参数</Button>
    <DataTable :value="tableData" tableStyle="min-width: 20rem">
      <Column field="field" header="参数"></Column>
      <Column field="description" header="描述"></Column>
      <Column field="operate" header="操作">
        <template #body="scope">
          <Button
            icon="pi pi-file-edit"
            variant="text"
            rounded
            aria-label="Cancel"
            size="normal"
            @click.stop="open(scope.data, scope.index)"
          />
          <Button
            icon="pi pi-times-circle"
            variant="text"
            rounded
            aria-label="Cancel"
            size="normal"
            @click="deleteParameter(scope.data)"
          />
        </template>
      </Column>
    </DataTable>

    <CreateParameter ref="createParameterRef" @submit="submit"></CreateParameter>
  </Fieldset>
</template>
<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { ref, computed } from 'vue'
import CreateParameter from './CreateParameter.vue'
const createParameterRef = ref<InstanceType<typeof CreateParameter>>()
const props = defineProps<{ parameters: Array<any>; updateFieldList: () => void }>()
const emit = defineEmits(['update:parameters'])
const tableData = computed({
  get: () => {
    if (!props.parameters) {
      emit('update:parameters', [])
    }
    return props.parameters
  },
  set: (event: any) => {
    emit('update:parameters', event)
  }
})
const open = (row?: any, index?: number) => {
  createParameterRef.value?.open(row, index)
}
const submit = (event: any) => {
  if (event.edit) {
    console.log(event)
    tableData.value[event.index] = event.row
  } else {
    if (tableData.value.some((row: any) => row.field == event.row.field)) {
      ElMessage.warning('字段已存在')
      return
    }
    tableData.value.push(event.row)
  }
  createParameterRef.value?.close()
  props.updateFieldList()
}

const deleteParameter = (row: any) => {
  tableData.value = tableData.value.filter((item: any) => item.field != row.field)
  props.updateFieldList()
}
</script>
<style lang="scss" scoped></style>
