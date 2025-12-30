<template>
  <el-dialog v-model="visible" title="创建处理器" width="800" :before-close="close">
    <el-form
      ref="createProcessorRef"
      style="max-width: 600px"
      :model="processor"
      label-width="auto"
      label-position="top"
      :rules="rules"
    >
      <el-form-item label="处理器名称" prop="name">
        <el-input v-model="processor.name" placeholder="请输入处理器名称" />
      </el-form-item>
      <el-form-item label="处理器描述" prop="desc">
        <el-input v-model="processor.desc" placeholder="请输入处理器描述" />
      </el-form-item>
      <el-form-item label="处理器协议" prop="protocol">
        <el-select v-model="processor.protocol" placeholder="请选择处理器协议">
          <el-option
            v-for="item in options"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="submit"> 添加 </el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import type { CreateProcessorVO } from '@/api/type/processor'
import processorAPI from '@/api/processor'
import { useRouter } from 'vue-router'
const router = useRouter()
const visible = ref<boolean>()
const options = [
  {
    label: 'HTTP',
    value: 'HTTP'
  }
]
const processor = ref<CreateProcessorVO>({
  name: '',
  desc: '',
  protocol: 'HTTP'
})
const currentProjectId = ref<string>()
const createProcessorRef = ref<FormInstance>()
const rules = reactive<FormRules<CreateProcessorVO>>({
  name: [{ required: true, message: '处理器名称必填', trigger: 'blur' }],
  desc: [{ required: true, message: '处理器描述必填', trigger: 'blur' }],
  protocol: [{ required: true, message: '请选择协议', trigger: 'change' }]
})
const submit = () => {
  createProcessorRef.value?.validate().then(() => {
    if (currentProjectId.value) {
      processorAPI.createProcessor(currentProjectId.value, processor.value).then((ok) => {
        ElMessage.success('创建成功')
        router.push({ name: 'processorWorkflow', params: { processorId: ok.data.id } })
      })
    }
  })
}
const open = (projectId: string) => {
  visible.value = true
  currentProjectId.value = projectId
}

const close = () => {
  visible.value = false
}

defineExpose({ open, close })
</script>
<style lang="scss" scoped></style>
