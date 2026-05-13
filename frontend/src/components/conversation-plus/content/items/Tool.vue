<template>
  <component :is="kw[toolName] || kw.default" :content="content" :loading="loading" :expanded="isExpanded" @toggle="isExpanded = !isExpanded" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import Terminal from './tool/Terminal.vue'
import Patch from './tool/Patch.vue'
import ReadFile from './tool/ReadFile.vue'
import ListDir from './tool/ListDir.vue'
import Grep from './tool/Grep.vue'
import Glob from './tool/Glob.vue'
import DefaultTool from './tool/index.vue'

const props = defineProps<{ content: any }>()

const loading = computed(() => props.content.status === 'RUNNING')
const toolName = computed(() => (props.content.toolName || '').toLowerCase())
const isExpanded = ref(props.content.status === 'RUNNING')

const kw: Record<string, any> = {
  run_command: Terminal,
  apply_patch: Patch,
  read_file: ReadFile,
  list_dir: ListDir,
  grep: Grep,
  glob: Glob,
  default: DefaultTool
}
</script>
