<template>
  <SimpleNodeContainer ref="containerRef" :model="model" :validate="validate" :submit="submit">
    <Content ref="contentRef"></Content>
  </SimpleNodeContainer>
</template>
<script setup lang="ts">
import SimpleNodeContainer from '@/workflow/common/SimpleNodeContainer.vue'
import type { BaseNodeModel } from '@logicflow/core'
import Content from './content/index.vue'
import {inject, onMounted, ref} from 'vue'
import { useNodeValidator } from '@/workflow/common/useNodeValidator'
import {init} from "@/workflow/nodes/file-download-node/content";
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
const containerRef = ref<InstanceType<typeof SimpleNodeContainer>>()
const contentRef = ref<InstanceType<typeof Content>>()
const validate = () => contentRef.value ? contentRef.value.validate() : Promise.resolve(true)
const submit = () => contentRef.value ? contentRef.value.submit() : Promise.resolve(true)
useNodeValidator(model, containerRef)
onMounted(() => init({ model } as any))
</script>
<style lang="scss" scoped></style>
