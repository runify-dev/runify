<template>
  <div>
    <div class="flex items-center justify-between mb-3">
      <label class="text-sm font-semibold">响应类型</label>
      <SelectButton
        v-model="mode"
        :options="modeOptions"
        option-label="label"
        option-value="value"
        size="small"
        :allow-empty="false"
      />
    </div>
    <Application v-if="mode === 'chat'" ref="subRef" />
    <Processor v-else ref="subRef" />
  </div>
</template>

<script setup lang="ts">
import { inject, ref, watch } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
import Application from '../application/index.vue'
import Processor from '../processor/index.vue'

const modeOptions = [
  { label: '对话', value: 'chat' },
  { label: '处理器', value: 'processor' }
]

const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const subRef = ref<any>()

// 工具可被 chat 或处理器调用，两种数据响应都支持；模式单独存，避免被子编辑器写 nodeData 覆盖
const mode = ref<'chat' | 'processor'>(model.properties.toolResponseMode || 'processor')
watch(mode, (m) => {
  model.properties.toolResponseMode = m
})
model.properties.toolResponseMode = mode.value

const validate = () => (subRef.value ? subRef.value.validate() : Promise.resolve({ values: {}, errors: {} }))
const submit = () => (subRef.value ? subRef.value.submit() : Promise.resolve({}))

defineExpose({ validate, submit })
</script>

<style lang="scss" scoped></style>
