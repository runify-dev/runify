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
        d="M2 5C2 3.34315 3.34315 2 5 2H19C20.6569 2 22 3.34315 22 5V19C22 20.6569 20.6569 22 19 22H5C3.34315 22 2 20.6569 2 19V5ZM4 5C4 4.44772 4.44772 4 5 4H11V8H4V5ZM4 10H11V14H4V10ZM20 14V10H13V14H20ZM13 16H20V19C20 19.5523 19.5523 20 19 20H13V16ZM11 16V20H5C4.44772 20 4 19.5523 4 19V16H11ZM13 8H20V5C20 4.44772 19.5523 4 19 4H13V8Z"
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
  return props.editor?.isActive('table')
})
const activeState = computed(() => {
  return isActive.value ? 'on' : 'off'
})

const focus = () => {
  props.editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}
</script>
<style lang="scss" scoped></style>
