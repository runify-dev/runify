<template>
  <component :is="kw[lang]"></component>
</template>
<script setup lang="ts">
import JavaScriptCodeEditor from './javascript-code-editor/index.vue'
import SqlCodeEditor from './sql-code-editor/index.vue'
import JsonCodeEditor from './sql-code-editor/index.vue'
withDefaults(
  defineProps<{
    lang: string
  }>(),
  { lang: 'JAVASCRIPT' }
)
const kw: any = {
  SQL: SqlCodeEditor,
  JAVASCRIPT: JavaScriptCodeEditor,
  JSON: JsonCodeEditor
}
</script>
<style lang="scss" scoped></style>
