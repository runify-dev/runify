<template>
  <div class="grid grid-cols-2 gap-6 p-6 h-full">
    <!-- 左侧：构造器 -->
    <Card>
      <template #title>字段构造器</template>
      <template #content>
        <Constructor ref="constructorRef" />
        <div class="flex gap-2 mt-4">
          <Button label="添加字段" icon="pi pi-plus" @click="addField" />
          <Button label="预览表单" severity="success" icon="pi pi-eye" @click="previewForm" />
        </div>
      </template>
    </Card>

    <!-- 右侧：表单预览 -->
    <Card>
      <template #title>
        <div class="flex items-center justify-between">
          <span>表单预览</span>
          <Button
            v-if="fieldList.length > 0"
            label="获取数据"
            severity="info"
            size="small"
            icon="pi pi-download"
            @click="getFormData"
          />
        </div>
      </template>
      <template #content>
        <div v-if="fieldList.length === 0" class="text-muted-color text-center py-12">
          左侧构造器添加字段后，此处实时展示
        </div>
        <DynamicsFormPlus v-show="fieldList.length > 0" ref="formRef">
          <template #after="formValue">
            <Divider />
            <div class="text-xs text-muted-color">
              <div class="font-semibold mb-1">当前表单值：</div>
              <pre class="whitespace-pre-wrap break-all">{{ JSON.stringify(formValue, null, 2) }}</pre>
            </div>
          </template>
        </DynamicsFormPlus>

        <!-- 已添加字段列表 -->
        <div v-if="fieldList.length > 0" class="mt-4">
          <Divider />
          <div class="text-sm font-semibold mb-2">已添加字段 ({{ fieldList.length }})</div>
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
                <span v-if="item.required" class="text-red-500 text-xs">*必填</span>
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
    toast.add({ severity: 'error', summary: '表单校验失败', detail: '请检查构造器中的必填项', life: 3000 })
    return
  }

  const data = constructorRef.value.getData()
  console.log('getData:', data)

  if (!data.field) {
    toast.add({ severity: 'warn', summary: '字段名为空', detail: '请填写字段名', life: 3000 })
    return
  }

  if (fieldList.value.some((f) => f.field === data.field)) {
    toast.add({ severity: 'warn', summary: '字段名重复', detail: `"${data.field}" 已存在`, life: 3000 })
    return
  }

  const formField = toFormField(data)
  const newList = [...fieldList.value, formField]
  fieldList.value = newList

  await nextTick()
  formRef.value?.render(newList)

  toast.add({ severity: 'success', summary: '添加成功', detail: `字段 "${data.field}" 已添加`, life: 2000 })
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
    toast.add({ severity: 'success', summary: '校验通过', life: 2000 })
  } catch {
    toast.add({ severity: 'error', summary: '校验失败', life: 3000 })
  }
}
</script>

<style lang="scss" scoped></style>
