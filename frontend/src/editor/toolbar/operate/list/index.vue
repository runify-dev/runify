<template>
  <dropdown-menu :items="buttons">
    <button
      data-tooltip-state="closed"
      class="tiptap-button"
      aria-label="Format text as heading"
      type="button"
      data-style="ghost"
      :data-active-state="activeState"
      role="button"
      tabindex="-1"
      data-disabled="false"
      aria-pressed="false"
      id="radix-_R_kanpfiv5ubrb_"
      aria-haspopup="menu"
      aria-expanded="false"
      data-state="closed"
    >
      <component :is="iconComponent(current)"></component>
      <svg
        width="24"
        height="24"
        class="tiptap-button-dropdown-small"
        viewBox="0 0 24 24"
        fill="currentColor"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          fill-rule="evenodd"
          clip-rule="evenodd"
          d="M5.29289 8.29289C5.68342 7.90237 6.31658 7.90237 6.70711 8.29289L12 13.5858L17.2929 8.29289C17.6834 7.90237 18.3166 7.90237 18.7071 8.29289C19.0976 8.68342 19.0976 9.31658 18.7071 9.70711L12.7071 15.7071C12.3166 16.0976 11.6834 16.0976 11.2929 15.7071L5.29289 9.70711C4.90237 9.31658 4.90237 8.68342 5.29289 8.29289Z"
          fill="currentColor"
        ></path>
      </svg>
    </button>
    <template #item="scope">
      <button
        @click="focus(scope.item.value)"
        data-tooltip-state="closed"
        class="tiptap-button"
        aria-label="Format text as heading"
        type="button"
        data-style="ghost"
        :data-active-state="isActive ? (current == scope.item.value ? 'on' : 'off') : 'off'"
        role="button"
        tabindex="-1"
        data-disabled="false"
        aria-pressed="false"
        id="radix-_R_kanpfiv5ubrb_"
        aria-haspopup="menu"
        aria-expanded="false"
        data-state="closed"
      >
        <component :is="iconComponent(scope.item.value)"></component>
        {{ scope.item.label }}
      </button>
    </template>
  </dropdown-menu>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { iconComponent } from './icons/index.ts'
import type { Editor } from '@tiptap/vue-3'
const props = defineProps<{ editor: Editor }>()
const buttons = [
  { value: 'bulletList', label: '符号列表' },
  { value: 'orderedList', label: '编号列表' },
  { value: 'taskList', label: '任务列表' }
]
const isBulletListActive = computed(() => {
  return props.editor?.isActive('bulletList')
})

const isOrderedListActive = computed(() => {
  return props.editor?.isActive('orderedList')
})

const isTaskListActive = computed(() => {
  return props.editor?.isActive('taskList')
})
const isActive = computed(() => {
  return isTaskListActive.value || isBulletListActive.value || isOrderedListActive.value
})
const current = computed(() => {
  if (isTaskListActive.value) {
    return 'taskList'
  } else if (isOrderedListActive.value) {
    return 'orderedList'
  } else {
    return 'bulletList'
  }
})
const activeState = computed(() => {
  return isActive.value ? 'on' : 'off'
})
const focus = (name: 'taskList' | 'orderedList' | 'bulletList' | string) => {
  switch (name) {
    case 'bulletList':
      props.editor.chain().focus().toggleBulletList().run()
      return
    case 'orderedList':
      props.editor.chain().focus().toggleOrderedList().run()
      return
    case 'taskList':
      props.editor.chain().focus().toggleList('taskList', 'taskItem').run()
      return
  }
}
</script>
<style lang="scss"></style>
