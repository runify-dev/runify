<template>
  <Dialog v-model:visible="visible" :header="dialogTitle" :style="{ width: '40rem' }" modal>
    <Form ref="formRef">
      <FormField v-slot="$field" name="field" initial-value="" :resolver="resolvers.field">
        <IftaLabel>
          <InputText type="text" fluid />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
          <label>参数</label>
        </IftaLabel>
      </FormField>

      <FormField
        v-slot="$field"
        class="mt-4"
        name="description"
        initial-value=""
        :resolver="resolvers.description"
      >
        <IftaLabel>
          <InputText type="text" fluid />
          <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
            $field.error?.message
          }}</Message>
          <label>描述</label>
        </IftaLabel>
      </FormField>

      <FormField
        v-slot="$field"
        class="mt-4"
        name="required"
        :initial-value="false"
        :resolver="resolvers.required"
      >
        <div class="p-d-flex p-ai-center">
          <div class="mt-2">
            <label class="p-mr-3">必填</label>
          </div>

          <ToggleSwitch />
        </div>
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>

      <FormField
        v-slot="$field"
        class="mt-4"
        name="type"
        initial-value="string"
        :resolver="resolvers.type"
      >
        <label class="p-d-block p-mb-2">类型</label>
        <SelectButton
          :options="typeOptions"
          option-label="label"
          option-value="value"
          fluid
          @change="handleTypeChange"
        />
        <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">{{
          $field.error?.message
        }}</Message>
      </FormField>

      <!-- 默认值输入 - 根据类型动态显示 -->
      <FormField
        v-if="selectedType !== 'object' && selectedType !== 'array'"
        v-slot="$field"
        class="mt-4"
        name="defaultValue"
        :resolver="resolvers.defaultValue"
      >
        <div>
          <label>默认值</label>
        </div>

        <component :is="getDefaultValueInput()" :type="getInputType()" fluid />
      </FormField>

      <!-- 对象类型的提示 -->
      <div v-if="selectedType === 'object'" class="mt-4 p-p-3 object-hint">
        <i class="pi pi-info-circle p-mr-2"></i>
        <span>对象类型可以在保存后添加子字段</span>
      </div>

      <!-- 数组类型的配置 -->
      <div v-if="selectedType === 'array'" class="mt-4">
        <FormField v-slot="$field: any" name="arrayItemType" :initial-value="'string'">
          <label class="p-d-block p-mb-2">数组项类型</label>
          <Select
            :options="arrayItemTypes"
            option-label="label"
            option-value="value"
            placeholder="选择数组项类型"
            fluid
          />
        </FormField>
      </div>

      <!-- 显示当前父级信息 -->
      <div v-if="parentField" class="mt-4 p-p-3 parent-info">
        <i class="pi pi-folder-open"></i>
        <span
          >父级字段: <strong>{{ parentField.field }}</strong> ({{ parentField.description }})</span
        >
      </div>
    </Form>

    <template #footer>
      <div class="dialog-footer">
        <Button @click="close">取消</Button>
        <Button @click="submit"> 提交 </Button>
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { nextTick, ref, computed, watch } from 'vue'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { FormInstance } from '@primevue/forms'
import Dialog from 'primevue/dialog'
import Form from '@primevue/forms/form'
import FormField from '@primevue/forms/formfield'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import IftaLabel from 'primevue/iftalabel'
import ToggleSwitch from 'primevue/toggleswitch'
import SelectButton from 'primevue/selectbutton'
import Select from 'primevue/select'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
const visible = ref<boolean>(false)
const formRef = ref<FormInstance>()
const edit = ref<boolean>(false)
const currentKey = ref<string>()
const selectedType = ref<string>('string')
const defaultValueModel = ref<any>()

const props = defineProps<{
  parentField?: any // 父级字段，用于添加子字段时标识
}>()

const emit = defineEmits(['submit'])

// 类型选项
const typeOptions = [
  { label: 'String', value: 'string' },
  { label: 'Integer', value: 'integer' },
  { label: 'Number', value: 'number' },
  { label: 'Boolean', value: 'boolean' },
  { label: 'Object', value: 'object' },
  { label: 'Array', value: 'array' }
]

// 数组项类型选项
const arrayItemTypes = [
  { label: 'String', value: 'string' },
  { label: 'Integer', value: 'integer' },
  { label: 'Number', value: 'number' },
  { label: 'Boolean', value: 'boolean' },
  { label: 'Object', value: 'object' }
]

// 验证规则
const resolvers = {
  field: zodResolver(
    z
      .string()
      .min(1, { message: '请输入参数' })
      .regex(/^[a-zA-Z_][a-zA-Z0-9_]*$/, {
        message: '参数名必须以字母或下划线开头，只能包含字母、数字和下划线'
      })
  ),
  description: zodResolver(z.string().min(1, { message: '请输入描述' })),
  required: zodResolver(z.boolean()),
  type: zodResolver(z.string().min(1, { message: '请选择参数类型' })),
  defaultValue: zodResolver(z.any().optional()),
  arrayItemType: zodResolver(z.string().optional())
}

// 对话框标题
const dialogTitle = computed(() => {
  if (edit.value) return '编辑参数'
  if (props.parentField) return `为 "${props.parentField.field}" 添加子字段`
  return '添加参数'
})

// 获取默认值输入组件
const getDefaultValueInput = () => {
  switch (selectedType.value) {
    case 'boolean':
      return ToggleSwitch
    case 'integer':
      return InputNumber
    default:
      return InputText
  }
}

// 获取输入类型
const getInputType = () => {
  switch (selectedType.value) {
    case 'integer':
    case 'number':
      return 'number'
    default:
      return 'text'
  }
}

// 处理类型变更
const handleTypeChange = (event: any) => {
  selectedType.value = event.value
  defaultValueModel.value = getDefaultForType(selectedType.value)
}

// 根据类型获取默认值
const getDefaultForType = (type: string) => {
  switch (type) {
    case 'string':
      return ''
    case 'integer':
    case 'number':
      return 0
    case 'boolean':
      return false
    case 'object':
      return undefined
    case 'array':
      return []
    default:
      return ''
  }
}

// 打开对话框
const open = (row?: any, key?: string) => {
  console.log(row)
  visible.value = true
  edit.value = !!(row && key !== undefined)
  currentKey.value = key

  if (row) {
    selectedType.value = row.type || 'string'

    nextTick(() => {
      formRef.value?.setValues({
        field: row.field,
        description: row.description,
        required: row.required,
        type: row.type,
        defaultValue: row.defaultValue,
        arrayItemType: row.arrayItemType || 'string'
      })
    })
  } else {
    selectedType.value = 'string'
    nextTick(() => {
      formRef.value?.reset()
    })
  }
}

// 关闭对话框
const close = () => {
  visible.value = false
  edit.value = false
  currentKey.value = undefined
  selectedType.value = 'string'
}

// 提交表单
const submit = () => {
  formRef.value?.validate().then(({ values, errors }) => {
    if (Object.keys(errors).length === 0) {
      const result = {
        edit: edit.value,
        key: currentKey.value,
        row: {
          ...values,
          // 对象类型初始化 children 数组
          ...(values.type === 'object' ? { children: [] } : {}),
          // 数组类型保存数组项类型
          ...(values.type === 'array'
            ? {
                arrayItemType: values.arrayItemType || 'string',
                defaultValue: values.defaultValue || []
              }
            : {})
        }
      }
      console.log(result)
      emit('submit', result)
      close()
    }
  })
}

// 暴露方法给父组件
defineExpose({
  open,
  close
})
</script>

<style lang="scss" scoped>
.object-hint,
.parent-info {
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 0px solid #3b82f6;
  color: #495057;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
}

.object-hint {
  border-left-color: #f59e0b;
  background-color: #fffbeb;
}

.parent-info {
  border-left-color: #10b981;
  background-color: #f0fdf4;
}

:deep(.p-selectbutton) {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
</style>
