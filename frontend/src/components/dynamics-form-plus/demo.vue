<template>
  <div class="grid grid-cols-2 gap-6 p-6 h-full">
    <!-- 左侧：构造器 -->
    <Card>
      <template #title>{{ t('dynamicsForm.demo.constructorTitle') }}</template>
      <template #content>
        <Constructor ref="constructorRef" />
        <div class="flex gap-2 mt-4">
          <Button :label="t('dynamicsForm.demo.addField')" icon="pi pi-plus" @click="addField" />
          <Button :label="t('dynamicsForm.demo.previewForm')" severity="success" icon="pi pi-eye" @click="previewForm" />
        </div>
      </template>
    </Card>

    <!-- 右侧：表单预览 -->
    <Card>
      <template #title>
        <div class="flex items-center justify-between">
          <span>{{ t('dynamicsForm.demo.formPreview') }}</span>
          <Button
            v-if="fieldList.length > 0"
            :label="t('dynamicsForm.demo.getData')"
            severity="info"
            size="small"
            icon="pi pi-download"
            @click="getFormData"
          />
        </div>
      </template>
      <template #content>
        <div v-if="fieldList.length === 0" class="text-muted-color text-center py-12">
          {{ t('dynamicsForm.demo.emptyHint') }}
        </div>
        <DynamicsFormPlus v-show="fieldList.length > 0" ref="formRef">
          <template #after="formValue">
            <Divider />
            <div class="text-xs text-muted-color">
              <div class="font-semibold mb-1">{{ t('dynamicsForm.demo.currentFormValue') }}</div>
              <pre class="whitespace-pre-wrap break-all">{{ JSON.stringify(formValue, null, 2) }}</pre>
            </div>
          </template>
        </DynamicsFormPlus>

        <!-- 已添加字段列表 -->
        <div v-if="fieldList.length > 0" class="mt-4">
          <Divider />
          <div class="text-sm font-semibold mb-2">{{ t('dynamicsForm.demo.addedFields') }} ({{ fieldList.length }})</div>
          <div class="flex flex-col gap-1">
            <div
              v-for="(item, index) in fieldList"
              :key="item.field"
              class="flex items-center justify-between px-3 py-2 bg-surface-50 rounded text-sm"
            >
              <div class="flex items-center gap-2">
                <Tag :value="item.type" severity="info" />
                <span>{{ item.field }}</span>
                <span class="text-muted-color">-</span>
                <span class="text-muted-color">{{ typeof item.label === 'object' ? item.label.value : item.label }}</span>
                <span v-if="item.required" class="text-red-500 text-xs">*{{ t('dynamicsForm.demo.required') }}</span>
              </div>
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                size="small"
                @click="removeField(index)"
              />
            </div>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { t } from '@/locales'
import DynamicsFormPlus from './index.vue'
import Constructor from './constructor/index.vue'
import type { FormField } from './type'
import { useToast } from 'primevue/usetoast'

defineOptions({ name: 'DynamicsFormPlusDemo' })

const toast = useToast()
const constructorRef = ref<InstanceType<typeof Constructor>>()
const formRef = ref<InstanceType<typeof DynamicsFormPlus>>()
const fieldList = ref<FormField[]>([])

const toFormField = (data: any): FormField => {
  return {
    field: data.field,
    type: data.type,
    label: data.label && typeof data.label === 'object'
      ? data.label
      : { type: 'TextLabel', value: data.label || data.field },
    required: data.required ?? false,
    defaultValue: data.defaultValue,
    showDefaultValue: data.showDefaultValue,
    attrs: data.attrs,
    propsInfo: data.props_info ? { resolver: data.props_info.resolver } : undefined,
    labelField: data.labelField,
    valueField: data.valueField,
    optionList: data.optionList
  }
}

const addField = async () => {
  if (!constructorRef.value) {
    console.warn('constructorRef is null')
    return
  }

  try {
    const valid = await constructorRef.value.validate()
    console.log('validate result:', valid)
  } catch (e) {
    console.error('validate error:', e)
    toast.add({ severity: 'error', summary: t('dynamicsForm.demo.validateFailed'), detail: t('dynamicsForm.demo.validateFailedDetail'), life: 3000 })
    return
  }

  const data = constructorRef.value.getData()
  console.log('getData:', data)

  if (!data.field) {
    toast.add({ severity: 'warn', summary: t('dynamicsForm.demo.fieldNameEmpty'), detail: t('dynamicsForm.demo.fieldNameEmptyDetail'), life: 3000 })
    return
  }

  if (fieldList.value.some((f) => f.field === data.field)) {
    toast.add({ severity: 'warn', summary: t('dynamicsForm.demo.fieldNameDuplicate'), detail: `"${data.field}" ${t('dynamicsForm.demo.fieldNameExists')}`, life: 3000 })
    return
  }

  const formField = toFormField(data)
  const newList = [...fieldList.value, formField]
  fieldList.value = newList

  await nextTick()
  formRef.value?.render(newList)

  toast.add({ severity: 'success', summary: t('dynamicsForm.demo.addSuccess'), detail: `${t('dynamicsForm.demo.addedFields')} "${data.field}" ${t('dynamicsForm.demo.fieldAdded')}`, life: 2000 })
}

const removeField = (index: number) => {
  const newList = fieldList.value.filter((_, i) => i !== index)
  fieldList.value = newList
  nextTick(() => {
    formRef.value?.render(newList)
  })
}

const previewForm = () => {
  formRef.value?.render(fieldList.value)
}

const getFormData = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    toast.add({ severity: 'success', summary: t('dynamicsForm.demo.validatePassed'), life: 2000 })
  } catch {
    toast.add({ severity: 'error', summary: t('dynamicsForm.demo.validateError'), life: 3000 })
  }
}
</script>

<style lang="scss" scoped></style>
