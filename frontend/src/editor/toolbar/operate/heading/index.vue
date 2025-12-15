<template>
  <el-dropdown trigger="click">
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
      <component :is="iconComponent(currentHeading)"></component>
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
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item @click="setHeading(button.level)" v-for="button in buttons">
          <button
            data-tooltip-state="closed"
            class="tiptap-button"
            aria-label="Format text as heading"
            type="button"
            data-style="ghost"
            :data-active-state="currentHeading == 'heading' + button.level ? 'on' : 'off'"
            role="button"
            tabindex="-1"
            data-disabled="false"
            aria-pressed="false"
            id="radix-_R_kanpfiv5ubrb_"
            aria-haspopup="menu"
            aria-expanded="false"
            data-state="closed"
          >
            <component :is="iconComponent('heading' + button.level)"></component>
            {{ button.label }}
          </button>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { iconComponent } from './icons/index.ts'
const props = defineProps<{ editor: Editor }>()

const activeHeading = computed(() => {
  return props.editor?.isActive('heading')
})
const buttons = [
  { level: 1, label: '标题1' },
  { level: 2, label: '标题2' },
  { level: 3, label: '标题3' },
  { level: 4, label: '标题4' },
  { level: 5, label: '标题5' },
  { level: 6, label: '标题6' },
]
const activeState = computed(() => {
  return activeHeading.value ? 'on' : 'off'
})
const currentHeading = computed(() => {
  if (activeHeading.value) {
    return 'heading' + props.editor.getAttributes('heading').level
  }
  return 'heading'
})
const setHeading = (level) => {
  if (currentHeading.value === 'heading' + level) {
    // 如果已经是该级别，切换回段落
    props.editor?.chain().focus().setParagraph().run()
  } else {
    props.editor?.chain().focus().toggleHeading({ level }).run()
  }
}
</script>
<style lang="scss"></style>
