<template>
  <el-dialog v-model="visible" :title="edit ? '修改' : '添加'" width="500">
    <el-form
      :model="form"
      label-width="auto"
      label-position="top"
      require-asterisk-position="right"
      :rules="rules"
      ref="formRef"
    >
      <el-form-item label="参数" prop="field">
        <el-input v-model="form.field" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" />
      </el-form-item>
      <el-form-item label="必填" prop="required">
        <el-switch v-model="form.required" />
      </el-form-item>
      <el-form-item label="位置" prop="location">
        <el-segmented v-model="form.location" :options="locationOptions" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="submit"> 提交 </el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { type FormRules, type FormInstance } from 'element-plus'

const visible = ref<boolean>(false)
const locationOptions = [
  { label: '引用', value: 'reference' },
  { label: '自定义', value: 'customize' }
]
// const typenOptions = ['string', 'integer', 'uuid', 'long', 'double']
interface Parameters {
  field: string
  description: string
  required: boolean
  location: 'reference' | 'customize'
  type: 'string' | 'integer' | 'uuid' | 'long' | 'double'
}
const defaultValue: Parameters = {
  field: '',
  description: '',
  required: false,
  location: 'reference',
  type: 'string'
}
const formRef = ref<FormInstance>()
const emit = defineEmits(['submit'])
const edit = ref<boolean>(false)
const form = ref<Parameters>({ ...defaultValue })
const close = () => {
  form.value = { ...defaultValue }
  visible.value = false
  edit.value = false
}
const open = (row?: Parameters) => {
  visible.value = true
  if (row) {
    form.value = row
    edit.value = true
  }
}
const rules = ref<FormRules<Parameters>>({
  field: [{ required: true, message: '请输入参数', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  required: [{ required: true, message: '请选择是否必填', trigger: 'change' }],
  location: [{ required: true, message: '请选择参数地址', trigger: 'change' }],
  type: [{ required: true, message: '请选择参数类型', trigger: 'change' }]
})
const submit = () => {
  formRef.value?.validate().then(() => {
    emit('submit', {
      edit: edit.value,
      row: form.value
    })
  })
}

defineExpose({
  open,
  close
})
</script>
<style lang="scss" scoped></style>
