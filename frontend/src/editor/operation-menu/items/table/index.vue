<template>
  <div class="bubble-menu">
    <dropdown-menu :items="buttons">
      <button class="tiptap-button" type="button" data-style="ghost" role="button">
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
            d="M10 12C10 10.8954 10.8954 10 12 10C13.1046 10 14 10.8954 14 12C14 13.1046 13.1046 14 12 14C10.8954 14 10 13.1046 10 12Z"
            fill="currentColor"
          ></path>
          <path
            fill-rule="evenodd"
            clip-rule="evenodd"
            d="M10 5C10 3.89543 10.8954 3 12 3C13.1046 3 14 3.89543 14 5C14 6.10457 13.1046 7 12 7C10.8954 7 10 6.10457 10 5Z"
            fill="currentColor"
          ></path>
          <path
            fill-rule="evenodd"
            clip-rule="evenodd"
            d="M10 19C10 17.8954 10.8954 17 12 17C13.1046 17 14 17.8954 14 19C14 20.1046 13.1046 21 12 21C10.8954 21 10 20.1046 10 19Z"
            fill="currentColor"
          ></path>
        </svg>
      </button>
      <template #item="scope">
        <div class="cursor-pointer w-full" @click="scope.item.handle()">{{ scope.item.label }}</div>
      </template>
    </dropdown-menu>
  </div>
</template>
<script setup lang="ts">
import { type Editor } from '@tiptap/vue-3'

const buttons = [
  {
    value: 'addRowAfter',
    label: '添加行',
    handle: () => {
      props.editor.chain().focus().addRowAfter().run()
    }
  },
  {
    value: 'addColumnAfter',
    label: '添加列',
    handle: () => {
      props.editor.chain()?.focus().addColumnAfter().run()
    }
  },
  {
    value: 'deleteRow',
    label: '删除行',
    handle: () => {
      props.editor.chain().focus().deleteColumn().run()
    }
  },
  {
    value: 'deleteColumn',
    label: '删除列',
    handle: () => {
      props.editor.chain().focus().deleteRow().run()
    }
  },
  {
    value: 'deleteTable',
    label: '删除',
    handle: () => {
      props.editor.chain().focus().deleteTable().run()
    }
  }
]
const props = defineProps<{ editor: Editor }>()
</script>
<style lang="scss" scoped></style>
