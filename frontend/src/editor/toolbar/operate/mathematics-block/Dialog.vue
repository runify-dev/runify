<template>
  <el-dialog @click.stop v-model="visible" title="Tips" width="500" :before-close="close">
    <el-form
      ref="ruleFormRef"
      style="max-width: 600px"
      :model="ruleForm"
      :rules="rules"
      label-width="auto"
      label-position="top"
    >
      <el-input
        v-model="ruleForm.mathematics"
        style="width: 100%"
        :rows="2"
        type="textarea"
        placeholder="Please mathematics"
      />
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">Cancel</el-button>
        <el-button type="primary" @click.stop="submit"> Confirm </el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { type Editor } from '@tiptap/vue-3'
import type { FormInstance, FormRules } from 'element-plus'

const visible = ref<boolean>()
const current = ref<Editor>()
const close = () => {
  current.value = undefined
  ruleForm.mathematics = ''
  visible.value = false
}
const open = (editor: Editor, mathematics?: string) => {
  visible.value = true
  current.value = editor
  ruleForm.mathematics = mathematics
}
const ruleForm = reactive<any>({
  mathematics: ''
})
const ruleFormRef = ref<FormInstance>()

const submit = () => {
  ruleFormRef.value?.validate().then(() => {
    current.value?.chain().insertBlockMath({ latex: ruleForm.mathematics }).focus().run()
    close()
  })
}
const rules = reactive<FormRules<any>>({
  mathematics: [{ required: true, message: 'Please mathematics', trigger: 'blur' }]
})
defineExpose({ open, close })
</script>
<style lang="scss" scoped></style>
