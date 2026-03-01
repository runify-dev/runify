<template>
  <Fieldset legend="参数设置">
    <Button variant="text" rounded aria-label="Filter" @click="open()"> 添加 </Button>
    <Form ref="formRef">
      <FormField
        class="mt-2"
        v-slot="$field"
        v-for="(value, index) in data"
        :key="value.field"
        :name="`parameters.${index}.value`"
        :initial-value="[]"
      >
        <label> {{ value.field }}</label>

        <div class="flex">
          <Cascader
            v-if="value.location === 'reference'"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="options"
            v-model="value.value"
            optionLabel="label"
            :optionGroupChildren="['children']"
            class="w-full"
            placeholder="请选择引用参数"
          />
          <InputText v-else type="text" fluid />
          <Button
            icon="pi pi-trash"
            variant="text"
            rounded
            aria-label="Filter"
            @click="deleteParameter(value)"
          />
        </div>

        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>
    </Form>

    <CreateParameter ref="createParameterRef" @submit="submit"></CreateParameter>
  </Fieldset>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import CreateParameter from './CreateParameter.vue'
import { ElMessage } from 'element-plus'
import { computed } from 'vue'
import Cascader from '@/components/cascader/index.vue'
const props = defineProps<{
  parameters: Array<any>
}>()

const createParameterRef = ref<InstanceType<typeof CreateParameter>>()
const getNodeFieldOptions = inject('getNodeFieldOptions') as any
const options = getNodeFieldOptions()

const emit = defineEmits(['update:parameters'])
const data = computed({
  get: () => {
    if (!props.parameters) {
      emit('update:parameters', [])
    }
    return props.parameters
  },
  set: (e) => {
    emit('update:parameters', e)
  }
})
const open = (row?: any) => {
  createParameterRef.value?.open(row)
}
const submit = (event: any) => {
  if (event.edit) {
    for (const index in data.value) {
      if (data.value[index].field == event.row.field) {
        data.value[index] = event.row
        break
      }
    }
  } else {
    if (data.value.some((row: any) => row.field == event.row.field)) {
      ElMessage.warning('字段已存在')
      return
    }
    data.value.push(event.row)
    console.log(data.value)
  }
  createParameterRef.value?.close()
}

const deleteParameter = (row: any) => {
  data.value = data.value.filter((item: any) => item.field != row.field)
}
const parameterFormRef = ref()
const validate = () => {
  return parameterFormRef.value.validate()
}
defineExpose({ validate })
</script>
<style lang="scss" scoped></style>
