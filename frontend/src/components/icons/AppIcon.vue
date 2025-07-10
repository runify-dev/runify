<template>
  <component v-if="isIconfont" :is="Object.keys(iconMap).includes(name)
    ? iconMap[name].iconReader()
    : iconMap['404'].iconReader()
    " class="el-icon app-icon">
  </component>

  <el-icon v-else-if="name">
    <component :is="name" />
  </el-icon>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const modules: any = import.meta.glob('./data/*.ts', { eager: true })
const iconMap: any = { ...Object.keys(modules).map((key) => modules[key].default).reduce((pre, next) => ({ ...pre, ...next })) }

const props = withDefaults(
  defineProps<{
    name?: string
  }>(),
  {
    name: '404'
  }
)

const isIconfont = computed(() => props.name?.includes('app-'))
</script>

<style lang="scss" scoped></style>
