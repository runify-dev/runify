<template>
  <Avatar :image="instance?.icon ? instance.icon : logo" style="width: 20px" />
</template>
<script setup lang="ts">
import logo from '@/assets/workflow/context-save-node.svg'
defineProps<{
  instance?: { icon: string }
}>()
</script>
<style lang="scss" scoped></style>
