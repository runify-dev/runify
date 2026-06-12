<template>
  <div class="codemirror-editor w-full">
    <form @submit.prevent>
      <Codemirror
        v-model="data"
        ref="cmRef"
        :extensions="extensions"
        :style="codemirrorStyle"
        :tab-size="4"
        :autofocus="true"
        v-bind="$attrs"
      />
    </form>
    <div class="codemirror-editor__footer">
      <Button icon="pi pi-expand" variant="text" @click="open" rounded aria-label="Filter" />
    </div>
    <Dialog v-model:visible="dialogVisible" :header="title" style="width: 50rem">
      <form @submit.prevent>
        <Codemirror
          v-model="cloneContent"
          :extensions="extensions"
          :style="codemirrorStyle"
          :tab-size="4"
          :autofocus="true"
          style="
            height: calc(100dvh - 160px) !important;
            border: 1px solid var(--p-content-border-color);
            border-radius: 4px;
          "
        />
      </form>
      <template #footer>
        <Button @click="submit">{{ t('common.confirm') }}</Button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Codemirror } from 'vue-codemirror'
import { t } from '@/locales'
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'
import { EditorView } from '@codemirror/view'
const props = defineProps<{
  title: string
  modelValue: any
}>()
const emit = defineEmits(['update:modelValue'])

const data = computed({
  set: (value) => {
    emit('update:modelValue', value)
  },
  get: () => {
    return props.modelValue
  }
})
const extensions = [javascript(), oneDark, EditorView.lineWrapping]

const codemirrorStyle = {
  height: '210px!important',
  width: '100%'
}
const cmRef = ref<InstanceType<typeof Codemirror>>()
// 弹出框相关代码
const dialogVisible = ref<boolean>(false)

const cloneContent = ref<string>('')

const open = () => {
  cloneContent.value = props.modelValue
  dialogVisible.value = true
}

function submit() {
  data.value = cloneContent.value
  cloneContent.value = ''
  dialogVisible.value = false
}
</script>

<style lang="scss" scoped>
.codemirror-editor {
  position: relative;

  &__footer {
    position: absolute;
    bottom: 10px;
    right: 10px;
  }
}
</style>
