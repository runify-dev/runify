<template>
  <Think :thinking="content" :default-expanded="true" :loading="loading"> </Think>
</template>
<script setup lang="ts">
import Think from '@/components/thinking/index.vue'
import { computed } from 'vue'

const props = defineProps<{ content: any }>()

const content = computed(() => {
  return props.content.content
})
const loading = computed(() => {
  return props.content.status === 'RUNNING'
})
</script>
<style lang="scss" scoped>
.reasoning-md {
  padding-left: 8px;
  --md-color: #8b8b8b !important;
}
.el-collapse {
  border-top: none;
  border: none;
}

/* 去掉 el-collapse-item 底部边框 */
.el-collapse-item__wrap {
  border-bottom: none;
}
.el-collapse-item__content {
  padding-bottom: 10px;
}
/* 去掉最后一个 item 的额外边框（如果有） */
.el-collapse-item:last-child {
  margin-bottom: 0;
}
</style>
