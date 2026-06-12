<template>
  <Dialog v-model:visible="visible" :header="t('project.form.header')" :style="{ width: '28rem' }">
    <Form ref="formRef">
      <div class="flex flex-col gap-4">
        <!-- 处理器名称 -->
        <FormField v-slot="$field" name="name" initial-value="" :resolver="resolvers.name">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-surface-700">{{ t('project.form.name') }}</label>
            <InputText type="text" :placeholder="t('project.form.namePlaceholder')" fluid class="!text-sm" />
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </div>
        </FormField>

        <!-- 处理器描述 -->
        <FormField v-slot="$field" name="desc" initial-value="" :resolver="resolvers.desc">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-surface-700">{{ t('project.form.desc') }}</label>
            <Textarea
              :placeholder="t('project.form.descPlaceholder')"
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
            <label class="text-sm font-medium text-surface-700">{{ t('project.form.protocol') }}</label>
            <Select
              :options="options"
              optionLabel="label"
              optionValue="value"
              :placeholder="t('project.form.protocolPlaceholder')"
              fluid
              class="!text-sm"
            />
            <p class="text-xs text-surface-400">{{ t('project.form.protocolDesc') }}</p>
            <Message v-if="$field?.invalid" severity="error" size="small" variant="simple">
              {{ $field.error?.message }}
            </Message>
          </div>
        </FormField>
      </div>
    </Form>

    <template #footer>
      <div class="flex justify-end gap-2">
        <Button :label="t('common.cancel')" severity="secondary" variant="outlined" @click="close" />
        <Button :label="t('common.create')" @click="submit" />
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
import { t } from '@/locales'

const resolvers = {
  name: zodResolver(z.string().min(1, { message: t('project.form.namePlaceholder') })),
  desc: zodResolver(z.string().optional()),
  protocol: zodResolver(z.string().min(1, { message: t('project.form.protocolPlaceholder') }))
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
          bus.emit('message:success', t('project.createSuccess'))
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
