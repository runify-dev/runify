<template lang="scss">
  <div></div>
</template>
<script setup lang="ts">
import { onMounted, inject } from 'vue'
import type { BaseNodeModel } from '@logicflow/core'
const getModel = inject('getModel') as () => BaseNodeModel
const model = getModel()
onMounted(() => {
  model.properties.field_list = [
    {
      label: '回答',
      value: 'content'
    }
  ]
})
</script>
<style lang="scss"></style>
