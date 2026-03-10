<template>
  <template>
    <bubble-menu
      v-for="(m, index) in menus"
      :key="index"
      :editor="editor"
      :should-show="() => m.shouldShow(editor)"
      :get-referenced-virtual-element="() => m.getReferencedVirtualElement(editor)"
      :options="{ ...m.options() }"
    >
      <component :is="m.compoent" :editor="editor"></component>
    </bubble-menu>
  </template>
</template>

<script setup lang="ts">
// @ts-expect-error
import { BubbleMenu } from '@tiptap/vue-3/menus'
import { type Editor } from '@tiptap/vue-3'
import { ref, onMounted, onBeforeUnmount } from 'vue'
import menus from './index.ts'

const props = defineProps<{ editor: Editor }>()

const scrollEl = ref<HTMLElement | null>(null)

const handleScroll = () => {
  props.editor.commands.setMeta('bubbleMenu', 'updatePosition')
}

onMounted(() => {
  scrollEl.value = document.querySelector('.layout-content-container')
  scrollEl.value?.addEventListener('scroll', handleScroll, { passive: true })
})

onBeforeUnmount(() => {
  scrollEl.value?.removeEventListener('scroll', handleScroll)
})
</script>
