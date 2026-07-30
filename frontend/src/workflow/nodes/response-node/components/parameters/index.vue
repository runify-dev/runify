<template>
  <div>
    <Button variant="text" rounded aria-label="Filter" @click="open()"> 添加 </Button>
    <Form ref="formRef">
      <FormField
        class="mt-2"
        v-slot="$field: any"
        v-for="(value, index) in data"
        :key="value.field"
        :name="value.location === 'reference' ? `${index}.reference` : `${index}.value`"
        :initial-value="value.location === 'reference' ? value.reference : value.value"
        :resolver="
          isRequired(value.required)
            ? zodResolver(
                value.location === 'reference'
                  ? z
                      .array(z.string(), { error: '请输入' + value.field })
                      .min(1, { error: '请输入' + value.field })
                  : z
                      .string({ error: '请输入' + value.field })
                      .min(1, { error: '请输入' + value.field })
              )
            : undefined
        "
      >
        <label> {{ value.field }}</label>
        <div class="flex mt-2">
          <Cascader
            v-if="value.location === 'reference'"
            :config="{ labelKey: 'label', valueKey: 'value' }"
            :options="options"
            v-bind:model-value="value.reference"
            @update:model-value="
              (v) => {
                $field.onChange({ value: v })
                value.reference = v
              }
            "
            optionLabel="label"
            :optionGroupChildren="['children']"
            class="w-full"
            placeholder="请选择引用参数"
          />
          <InputText v-else type="text" v-model="value.value" fluid />
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
  </div>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import CreateParameter from './CreateParameter.vue'
import { computed } from 'vue'
import Cascader from '@/components/cascader/index.vue'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { FormInstance } from '@primevue/forms'
import { isRequired } from '@/workflow/common/validator-utils'
import bus from '@/bus'
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
      bus.emit('message:warn', '字段已存在')
      return
    }
    data.value.push(event.row)
  }
  createParameterRef.value?.close()
}

const deleteParameter = (row: any) => {
  data.value = data.value.filter((item: any) => item.field != row.field)
}
const formRef = ref<FormInstance>()
const validate = () => {
  return formRef.value?.validate()
}
defineExpose({ validate })
</script>
<style lang="scss" scoped></style>
