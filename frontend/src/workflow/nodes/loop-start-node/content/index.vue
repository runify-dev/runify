<template>
  <div class="text-sm text-slate-500 py-2">
    循环体的起始节点，无需配置。
  </div>
</template>
<script setup lang="ts">
const validate = () => Promise.resolve({ values: {}, errors: {} })
const submit = () => Promise.resolve({})
defineExpose({ validate, submit })
</script>
<style lang="scss" scoped></style>
