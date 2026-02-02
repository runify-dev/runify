<template>
  <component :is="kw[row.protocol]" :row="row"></component>
</template>
<script setup lang="ts">
import PostgreSQL from './postgre-sql/index.vue'
defineProps<{
  row: any
}>()
const kw: any = {
  POSTGRESQL: PostgreSQL
}
</script>
<style lang="scss" scoped></style>
