<template>
  <div class="layout-breadcrumb">
    <Breadcrumb :home="home" :model="items" />
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'

const home = ref({
  icon: 'pi pi-home'
})
const items = ref([
  { label: 'Electronics' },
  { label: 'Computer' },
  { label: 'Accessories' },
  { label: 'Keyboard' },
  { label: 'Wireless' }
])
</script>
<style lang="scss" scoped></style>
