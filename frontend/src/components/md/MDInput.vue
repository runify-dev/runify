<template>
  <MdEditor
    :completions="completions"
    v-bind="$attrs"
    v-model="data"
    :preview="false"
    :toolbars="[]"
    class="magnify-md-editor"
    :footers="footers"
  >
    <template #defFooters>
      <el-button text type="info" @click="openDialog">
        <AppIcon class="color-secondary" name="app-magnify" style="font-size: 16px"></AppIcon>
      </el-button>
    </template>
  </MdEditor>
  <!-- 回复内容弹出层 -->
  <el-dialog v-model="dialogVisible" :title="title" append-to-body align-center>
    <MdEditor
      style="height: 300px"
      v-model="cloneContent"
      :preview="false"
      :toolbars="[]"
      :footers="[]"
    ></MdEditor>
    <template #footer>
      <div class="dialog-footer mt-24">
        <el-button type="primary" @click="submitDialog">提交</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, inject } from 'vue'
import { MdEditor } from 'md-editor-v3'
import AppIcon from '@/components/icons/AppIcon.vue'
import { type CompletionSource } from '@codemirror/autocomplete'
const getOptions = inject('getOptions') as any
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
const completions = ref<Array<CompletionSource>>([
  (context) => {
    const word = context.matchBefore(/@\w*/)
    if (word === null || (word.from == word.to && context.explicit)) {
      return null
    }

    if (getOptions) {
      const options: Array<any> = getOptions()
      return {
        from: word.from,
        options: options.map((option) => {
          return {
            ...option,
            apply: (view, option: any, from, to) => {
              // 替换触发词和当前文本
              view.dispatch({
                changes: {
                  from: from,
                  to,
                  insert: `${option.value}`
                }
              })
            }
          }
        })
      }
    }
    return null
  }
])

const cloneContent = ref('')
const footers: any = [null, '=', 0]
const openDialog = () => {
  cloneContent.value = props.modelValue
  dialogVisible.value = true
}
function submitDialog() {
  data.value = cloneContent.value
  dialogVisible.value = false
}
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
