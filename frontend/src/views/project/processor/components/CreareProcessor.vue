<template>
  <Dialog v-model:visible="visible" header="创建处理器" :style="{ width: '28rem' }">
    <Form ref="formRef">
      <div class="flex flex-col gap-4">
        <!-- 处理器名称 -->
        <FormField v-slot="$field" name="name" initial-value="" :resolver="resolvers.name">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-surface-700">处理器名称</label>
            <InputText type="text" placeholder="请输入处理器名称" fluid class="!text-sm" />
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </div>
        </FormField>

        <!-- 处理器描述 -->
        <FormField v-slot="$field" name="desc" initial-value="" :resolver="resolvers.desc">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-surface-700">处理器描述</label>
            <Textarea
              placeholder="请输入处理器描述（选填）"
              rows="3"
              fluid
              class="!text-sm !resize-none"
            />
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </div>
        </FormField>

        <!-- 协议类型 -->
        <FormField v-slot="$field" name="protocol" initial-value="HTTP" :resolver="resolvers.protocol">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-surface-700">协议类型</label>
            <Select
              :options="options"
              optionLabel="label"
              optionValue="value"
              placeholder="请选择协议类型"
              fluid
              class="!text-sm"
            />
            <p class="text-xs text-surface-400">选择处理器使用的通信协议</p>
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </div>
        </FormField>
      </div>
    </Form>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button label="取消" severity="secondary" variant="outlined" @click="close" />
        <Button label="创建" @click="submit" />
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import bus from '@/bus'
import processorAPI from '@/api/processor'
import { useRouter } from 'vue-router'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { z } from 'zod'
import type { FormInstance } from '@primevue/forms'

const resolvers = {
  name: zodResolver(z.string().min(1, { message: '请输入处理器名称' })),
  desc: zodResolver(z.string().optional()),
  protocol: zodResolver(z.string().min(1, { message: '请选择协议类型' }))
}

const router = useRouter()
const visible = ref(false)
const currentProjectId = ref<string>()
const formRef = ref<FormInstance>()

const options = [
  {
    label: 'HTTP',
    value: 'HTTP'
  }
]

const submit = () => {
  formRef.value?.validate().then(({ values, errors }) => {
    if (Object.keys(errors).length === 0) {
      if (currentProjectId.value) {
        processorAPI.createProcessor(currentProjectId.value, values).then((ok) => {
          bus.emit('message:success', '创建成功')
          router.push({ name: 'processorWorkflow', params: { processorId: ok.data.id } })
        })
      }
    }
  })
}

const open = (projectId: string) => {
  visible.value = true
  currentProjectId.value = projectId
}

const close = () => {
  visible.value = false
  currentProjectId.value = undefined
}

defineExpose({ open, close })
</script>
