<template>
  <div class="flex flex-col h-full min-h-0">
    <div class="flex items-center gap-2 px-4 py-2 border-b shrink-0" style="border-color: var(--p-content-border-color);">
      <i class="pi pi-code text-primary-500"/>
      <span class="text-sm font-semibold">{{ file.name }}</span>
      <span class="text-xs text-surface-400 ml-1">{{ language }}</span>
      <span v-if="saving" class="text-xs text-surface-400 ml-auto">{{ t('skill.details.saving') }}</span>
      <span v-else-if="saved" class="text-xs text-green-500 ml-auto">{{ t('skill.details.saved') }}</span>
    </div>
    <div class="code-editor-wrap" :class="{ 'cm-dark': isDarkTheme }">
      <Codemirror
        v-model="content"
        :extensions="extensions"
        :tab-size="4"
        :autofocus="true"
        @change="onChange"
      />
    </div>
  </div>
</template>
<script setup lang="ts">
import {computed, onBeforeUnmount, ref} from 'vue'
import {t} from '@/locales'
import {Codemirror} from 'vue-codemirror'
import {javascript} from '@codemirror/lang-javascript'
import {json} from '@codemirror/lang-json'
import {sql} from '@codemirror/lang-sql'
import {python} from '@codemirror/lang-python'
import {java} from '@codemirror/lang-java'
import {cpp} from '@codemirror/lang-cpp'
import {go} from '@codemirror/lang-go'
import {rust} from '@codemirror/lang-rust'
import {html} from '@codemirror/lang-html'
import {css} from '@codemirror/lang-css'
import {xml} from '@codemirror/lang-xml'
import {yaml} from '@codemirror/lang-yaml'
import {php} from '@codemirror/lang-php'
import {oneDark} from '@codemirror/theme-one-dark'
import {EditorView} from '@codemirror/view'
import {useLayout} from '@/layout-plus/index'
import skillApi from '@/api/skill'
import type {SkillFile} from '@/api/skill'

const props = defineProps<{ file: SkillFile; skillId: string }>()
const {isDarkTheme} = useLayout()

const EXT_LANG: Record<string, () => any> = {
  '.py': python,
  '.js': () => javascript(),
  '.jsx': () => javascript({jsx: true}),
  '.ts': () => javascript({typescript: true}),
  '.tsx': () => javascript({typescript: true, jsx: true}),
  '.json': json,
  '.sql': sql,
  '.java': java,
  '.c': cpp,
  '.cpp': cpp,
  '.h': cpp,
  '.go': go,
  '.rs': rust,
  '.html': html,
  '.css': css,
  '.xml': xml,
  '.yaml': yaml,
  '.yml': yaml,
  '.php': php,
}

const EXT_NAME: Record<string, string> = {
  '.py': 'python', '.js': 'javascript', '.ts': 'typescript',
  '.jsx': 'javascript', '.tsx': 'typescript', '.sh': 'shell',
  '.bash': 'shell', '.sql': 'sql', '.json': 'json',
  '.yaml': 'yaml', '.yml': 'yaml', '.xml': 'xml',
  '.html': 'html', '.css': 'css', '.java': 'java',
  '.go': 'go', '.rs': 'rust', '.c': 'c', '.cpp': 'cpp',
  '.h': 'c', '.rb': 'ruby', '.php': 'php', '.lua': 'lua',
  '.r': 'r', '.toml': 'toml', '.ini': 'ini',
}

const getExtension = (name: string): string => {
  const dot = name.lastIndexOf('.')
  return dot > 0 ? name.slice(dot).toLowerCase() : ''
}

const ext = getExtension(props.file.name)
const language = EXT_NAME[ext] || 'text'

const extensions = computed(() => {
  const langExt = EXT_LANG[ext]
  const base = langExt ? [langExt()] : []
  base.push(EditorView.lineWrapping)
  if (isDarkTheme.value) {
    base.push(oneDark)
  }
  return base
})

const content = ref(props.file.content || '')
let saveTimer: any = null
const saving = ref(false)
const saved = ref(false)

const onChange = () => {
  if (saveTimer) clearTimeout(saveTimer)
  saved.value = false
  saveTimer = setTimeout(() => {
    saving.value = true
    skillApi.updateContent(props.skillId, props.file.id, content.value).then(() => {
      saving.value = false
      saved.value = true
      setTimeout(() => { saved.value = false }, 2000)
    }).catch(() => { saving.value = false })
  }, 3000)
}

onBeforeUnmount(() => { if (saveTimer) clearTimeout(saveTimer) })
</script>
<style scoped>
.code-editor-wrap :deep(.cm-editor) {
  height: calc(var(--layout-main-height) - 80px);
  background: var(--p-content-background);
}
.code-editor-wrap :deep(.cm-editor.cm-focused) {
  outline: none;
}
.code-editor-wrap :deep(.cm-scroller) {
  font-family: 'JetBrains Mono NL', 'Fira Code', 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  padding: 4px 0;
}
.code-editor-wrap :deep(.cm-gutters) {
  background: var(--p-content-background);
  border-right: 1px solid var(--p-content-border-color);
  color: var(--p-text-muted-color);
}
.code-editor-wrap :deep(.cm-activeLineGutter) {
  background: var(--p-content-background);
}
.code-editor-wrap :deep(.cm-activeLine) {
  background: var(--p-surface-50);
}
.code-editor-wrap :deep(.cm-cursor) {
  border-left-color: var(--p-text-color);
}
.code-editor-wrap :deep(.cm-selectionBackground) {
  background: var(--p-primary-100) !important;
}
.code-editor-wrap.cm-dark :deep(.cm-activeLine) {
  background: var(--p-surface-800);
}
.code-editor-wrap.cm-dark :deep(.cm-gutters) {
  background: var(--p-surface-900);
  border-right-color: var(--p-surface-700);
  color: var(--p-surface-400);
}
.code-editor-wrap.cm-dark :deep(.cm-selectionBackground) {
  background: var(--p-primary-900) !important;
}
</style>
