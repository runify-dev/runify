<template>
  <slot v-bind:height="height"></slot>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
const props = withDefaults(defineProps<{ excludeHeight: number }>(), { excludeHeight: 0 })
const height = ref(window.innerHeight - props.excludeHeight)

const update = () => {
  height.value = window.innerHeight - props.excludeHeight
}

onMounted(() => window.addEventListener('resize', update))
onUnmounted(() => window.removeEventListener('resize', update))
</script>
<style lang="scss" scoped></style>
