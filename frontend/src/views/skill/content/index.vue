<template>
  <component :is="viewer" v-if="viewer" :file="file" :skillId="skillId" @save="$emit('save', $event)"/>
</template>
<script setup lang="ts">
import {computed} from 'vue'
import MarkdownEditor from './MarkdownEditor.vue'
import CodeEditor from './CodeEditor.vue'
import FileViewer from './FileViewer.vue'
import type {SkillFile} from '@/api/skill'

const props = defineProps<{ file: SkillFile; skillId: string }>()
defineEmits<{ save: [content: string] }>()

const CODE_EXTENSIONS: Record<string, string> = {
  '.py': 'python',
  '.js': 'javascript',
  '.ts': 'typescript',
  '.jsx': 'javascript',
  '.tsx': 'typescript',
  '.sh': 'shell',
  '.bash': 'shell',
  '.sql': 'sql',
  '.json': 'json',
  '.yaml': 'yaml',
  '.yml': 'yaml',
  '.xml': 'xml',
  '.html': 'html',
  '.css': 'css',
  '.java': 'java',
  '.go': 'go',
  '.rs': 'rust',
  '.c': 'c',
  '.cpp': 'cpp',
  '.h': 'c',
  '.rb': 'ruby',
  '.php': 'php',
  '.lua': 'lua',
  '.r': 'r',
  '.toml': 'toml',
  '.ini': 'ini',
  '.env': 'shell',
  '.dockerfile': 'dockerfile',
  '.makefile': 'makefile',
}

const getExtension = (name: string): string => {
  const dot = name.lastIndexOf('.')
  return dot > 0 ? name.slice(dot).toLowerCase() : ''
}

const viewer = computed(() => {
  if (props.file.type === 'file') return FileViewer
  const ext = getExtension(props.file.name)
  if (ext === '.md' || ext === '.markdown') return MarkdownEditor
  if (CODE_EXTENSIONS[ext]) return CodeEditor
  return CodeEditor
})

const language = computed(() => {
  const ext = getExtension(props.file.name)
  return CODE_EXTENSIONS[ext] || 'text'
})
</script>
