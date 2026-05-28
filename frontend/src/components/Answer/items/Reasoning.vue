<template>
  <el-collapse expand-icon-position="left" v-model="activeNames">
    <el-collapse-item
      title="思考过程"
      name="reasoning"
      style="--el-collapse-content-text-color: var(--p-text-muted-color)"
    >
      <MdPreview
        v-bind:model-value="content"
        ref="editorRef"
        editorId="preview-only"
        style="color: var(--p-text-muted-color)"
        class="reasoning-md"
      />
    </el-collapse-item>
  </el-collapse>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
const activeNames = ref<Array<string>>(['reasoning'])
const props = defineProps<{ chunk: any }>()
const content = computed(() => {
  return props.chunk.reasoning
})
</script>
<style lang="scss" scoped>
.reasoning-md {
  padding-left: 8px;
  --md-color: var(--p-text-muted-color) !important;
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
