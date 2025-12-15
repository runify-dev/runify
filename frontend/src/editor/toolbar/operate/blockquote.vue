<template>
  <button
    @click="focus"
    class="tiptap-button"
    type="button"
    data-style="ghost"
    role="button"
    :data-active-state="activeState"
  >
    <svg
      width="24"
      height="24"
      class="tiptap-button-icon"
      viewBox="0 0 24 24"
      fill="currentColor"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        fill-rule="evenodd"
        clip-rule="evenodd"
        d="M8 6C8 5.44772 8.44772 5 9 5H16C16.5523 5 17 5.44772 17 6C17 6.55228 16.5523 7 16 7H9C8.44772 7 8 6.55228 8 6Z"
        fill="currentColor"
      ></path>
      <path
        fill-rule="evenodd"
        clip-rule="evenodd"
        d="M4 3C4.55228 3 5 3.44772 5 4L5 20C5 20.5523 4.55229 21 4 21C3.44772 21 3 20.5523 3 20L3 4C3 3.44772 3.44772 3 4 3Z"
        fill="currentColor"
      ></path>
      <path
        fill-rule="evenodd"
        clip-rule="evenodd"
        d="M8 12C8 11.4477 8.44772 11 9 11H20C20.5523 11 21 11.4477 21 12C21 12.5523 20.5523 13 20 13H9C8.44772 13 8 12.5523 8 12Z"
        fill="currentColor"
      ></path>
      <path
        fill-rule="evenodd"
        clip-rule="evenodd"
        d="M8 18C8 17.4477 8.44772 17 9 17H16C16.5523 17 17 17.4477 17 18C17 18.5523 16.5523 19 16 19H9C8.44772 19 8 18.5523 8 18Z"
        fill="currentColor"
      ></path>
    </svg>
  </button>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { type Editor } from '@tiptap/vue-3'
const props = defineProps<{ editor: Editor }>()

const isActive = computed(() => {
  return props.editor?.isActive('blockquote')
})
const activeState = computed(() => {
  return isActive.value ? 'on' : 'off'
})

const focus = () => {
  props.editor.chain().focus().toggleBlockquote().run()
}
</script>
<style lang="scss" scoped></style>
