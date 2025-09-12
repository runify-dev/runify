<template>
  <div v-if="false"></div>
</template>
<script setup lang="ts">
import { inject, onMounted, getCurrentInstance } from 'vue'
const table = inject('table') as any
const props = defineProps<{ prop: any }>()
onMounted(() => {
  console.log('ss', table)
  const instance = getCurrentInstance()
  table.columns.value.push({
    prop: props.prop,
    uid: instance?.uid,
    headerSlot: instance?.slots.header,
    defaultSlot: instance?.slots.default
  })
})
</script>
<style lang=""></style>
