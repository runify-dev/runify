<template>
  <div class="code-preview">
    <div class="cp-toolbar">
      <span class="cp-lang">{{ langLabel }}</span>
      <button class="cp-copy" @click="copy">
        {{ copied ? '已复制' : '复制' }}
      </button>
    </div>
    <div class="cp-editor">
      <Codemirror
        :modelValue="modelValue"
        :extensions="extensions"
        :tab-size="2"
        :editable="false"
      />
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { Codemirror } from 'vue-codemirror'
import { json } from '@codemirror/lang-json'
import { javascript } from '@codemirror/lang-javascript'
import { sql } from '@codemirror/lang-sql'
import { xml } from '@codemirror/lang-xml'
import { markdown } from '@codemirror/lang-markdown'
import { python } from '@codemirror/lang-python'
import { html } from '@codemirror/lang-html'
import { css } from '@codemirror/lang-css'
import { yaml } from '@codemirror/lang-yaml'
import type { Extension } from '@codemirror/state'

const props = withDefaults(
  defineProps<{
    modelValue: string
    language?: string
    filename?: string
  }>(),
  {}
)

const copied = ref(false)

const EXT_MAP: Record<string, () => Extension> = {
  '.json': json,
  '.js': javascript,
  '.mjs': javascript,
  '.cjs': javascript,
  '.ts': () => javascript({ typescript: true }),
  '.mts': () => javascript({ typescript: true }),
  '.cts': () => javascript({ typescript: true }),
  '.jsx': () => javascript({ jsx: true }),
  '.tsx': () => javascript({ jsx: true, typescript: true }),
  '.sql': sql,
  '.xml': xml,
  '.svg': xml,
  '.md': markdown,
  '.markdown': markdown,
  '.py': python,
  '.html': html,
  '.htm': html,
  '.css': css,
  '.scss': css,
  '.less': css,
  '.yaml': yaml,
  '.yml': yaml
}

const LANG_NAME_MAP: Record<string, string> = {
  '.json': 'JSON',
  '.js': 'JavaScript',
  '.mjs': 'JavaScript',
  '.cjs': 'JavaScript',
  '.ts': 'TypeScript',
  '.mts': 'TypeScript',
  '.cts': 'TypeScript',
  '.jsx': 'JSX',
  '.tsx': 'TSX',
  '.sql': 'SQL',
  '.xml': 'XML',
  '.svg': 'SVG',
  '.md': 'Markdown',
  '.markdown': 'Markdown',
  '.py': 'Python',
  '.html': 'HTML',
  '.htm': 'HTML',
  '.css': 'CSS',
  '.scss': 'SCSS',
  '.less': 'Less',
  '.yaml': 'YAML',
  '.yml': 'YAML'
}

const ext = computed(() => {
  if (props.filename) {
    return '.' + props.filename.split('.').pop()?.toLowerCase()
  }
  if (props.language) {
    return '.' + props.language.toLowerCase()
  }
  return ''
})

const langLabel = computed(() => {
  if (ext.value && LANG_NAME_MAP[ext.value]) return LANG_NAME_MAP[ext.value]
  if (props.language) return props.language
  return 'Text'
})

const extensions = computed<Extension[]>(() => {
  const key = ext.value
  if (key && EXT_MAP[key]) return [EXT_MAP[key]()]
  return []
})

const copy = async () => {
  await navigator.clipboard.writeText(props.modelValue)
  copied.value = true
  setTimeout(() => (copied.value = false), 1500)
}
</script>
<style scoped>
.code-preview {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 8px;
}

.cp-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 10px;
  border-bottom: 1px solid var(--bd, #e5e5e3);
  background: var(--bg2, #f5f5f3);
  flex-shrink: 0;
}

.cp-lang {
  font-size: 11px;
  color: var(--t3, #9b9b9b);
}

.cp-copy {
  padding: 2px 8px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--t3, #9b9b9b);
  font-size: 11px;
  cursor: pointer;
  font-family: inherit;
}

.cp-copy:hover {
  background: var(--hv, #efefed);
  color: var(--t1, #37352f);
}

.cp-editor {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
  z-index: 0;
}

.cp-editor :deep(.cm-editor) {
  height: 100%;
  font-size: 13px;
}

.cp-editor :deep(.cm-scroller) {
  overflow: auto;
}

.cp-editor :deep(.cm-content) {
  font-family: 'JetBrains Mono NL', monospace;
}
</style>
