<template>
  <editor-content :editor="editor" />
  <AppIcon
    class="color-secondary"
    name="app-magnify"
    style="font-size: 16px"
    @click="openDialog"
  ></AppIcon>
  <!-- 回复内容弹出层 -->
  <Dialog v-model:visible="dialogVisible" :title="title" append-to-body align-center>
    <editor-content :editor="editor" />
    <template #footer>
      <div class="dialog-footer mt-24">
        <el-button type="primary" @click="submitDialog">提交</el-button>
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { EditorContent, Editor } from '@tiptap/vue-3'
import { ref, computed, watch, inject, reactive, onMounted } from 'vue'
import AppIcon from '@/components/icons/AppIcon.vue'
const getOptions = inject('getOptions') as any
import newInstance from '@/editor/editor/index'
const change = (v: string) => {
  data.value = v
}
const editor: Editor = reactive(newInstance('', change)) as Editor
defineOptions({ name: 'MdEditorMagnify' })
const props = defineProps<{
  title: String
  modelValue: any
}>()
const emit = defineEmits(['update:modelValue', 'submitDialog'])
const data = computed({
  set: (value) => {
    emit('update:modelValue', value)
  },
  get: () => {
    return props.modelValue
  }
})
const dialogVisible = ref(false)
watch(dialogVisible, (bool) => {
  if (!bool) {
    emit('submitDialog', cloneContent.value)
  }
})

const cloneContent = ref('')
const openDialog = () => {
  cloneContent.value = props.modelValue
  dialogVisible.value = true
}
function submitDialog() {
  data.value = cloneContent.value
  dialogVisible.value = false
}
onMounted(() => {
  editor.commands.setContent(data.value, { contentType: 'markdown' })
})
</script>

<style lang="scss" scoped>
.magnify-md-editor {
  :deep(.md-editor-footer) {
    border: none !important;
  }
}

.color-secondary {
  color: var(--app-text-color-secondary);
}
</style>
