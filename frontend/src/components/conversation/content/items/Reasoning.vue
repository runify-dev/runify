<template>
  <el-collapse expand-icon-position="left" v-model="activeNames">
    <el-collapse-item
      title="思考过程"
      name="reasoning"
      style="--el-collapse-content-text-color: #8b8b8b"
    >
      <editor-content :editor="editor" />
    </el-collapse-item>
  </el-collapse>
</template>
<script setup lang="ts">
import newInstance from '@/editor/editor/index'
import { computed, ref, reactive, watch } from 'vue'
import { EditorContent, Editor } from '@tiptap/vue-3'
const activeNames = ref<Array<string>>(['content'])
const props = defineProps<{ content: any }>()
const editor: Editor = reactive(newInstance('')) as Editor
const content = computed(() => {
  return props.content.content
})
watch(content, () => {
  editor.commands.setContent(content.value, { contentType: 'markdown' })
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
