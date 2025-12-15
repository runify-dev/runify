<template>
  <bubble-menu
    v-for="m in menus"
    :editor="editor"
    :should-show="() => m.shouldShow(editor)"
    :get-referenced-virtual-element="() => m.getReferencedVirtualElement(editor)"
    :options="m.options()"
  >
    <component :is="m.compoent" :editor="editor"></component>
  </bubble-menu>
</template>
<script setup lang="ts">
import { findParentNode, posToDOMRect } from '@tiptap/core'
import { BubbleMenu } from '@tiptap/vue-3/menus'
import { type Editor } from '@tiptap/vue-3'
import menus from './index.ts'
const props = defineProps<{
  editor: Editor
}>()

const getListVirtualElement = () => {
  const editor = props.editor
  const parentNode = findParentNode((node) => node.type.name === 'table')(editor.state.selection)
  if (parentNode) {
    const domRect = posToDOMRect(
      editor.view,
      parentNode.start,
      parentNode.start + parentNode.node.nodeSize,
    )
    return {
      getBoundingClientRect: () => domRect,
      getClientRects: () => [domRect],
    }
  }
  return null
}
const appendRow = () => {
  props.editor.chain().focus().addRowAfter().run()
}
</script>
<style lang="scss"></style>
