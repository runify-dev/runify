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
      viewBox="0 0 1024 1024"
      version="1.1"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M512 928H128a32 32 0 0 1-26.88-49.92L345.6 512 101.12 145.92A32 32 0 0 1 128 96h384a32 32 0 0 1 0 64H187.52l223.36 334.08a33.28 33.28 0 0 1 0 35.84L187.52 864H512a32 32 0 0 1 0 64zM640 928a36.48 36.48 0 0 1-17.92-5.12 32.64 32.64 0 0 1-8.96-44.8l256-384a32 32 0 0 1 53.76 35.84l-256 384a33.28 33.28 0 0 1-26.88 14.08z"
        fill="currentColor"
        p-id="4639"
      ></path>
      <path
        d="M896 928a33.28 33.28 0 0 1-26.88-14.08l-256-384a32 32 0 1 1 53.76-35.84l256 384a32.64 32.64 0 0 1-8.96 44.8 36.48 36.48 0 0 1-17.92 5.12z"
        fill="currentColor"
        p-id="4640"
      ></path>
    </svg>
    <MathematicsDialog ref="dialogRef"></MathematicsDialog>
  </button>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { type Editor } from '@tiptap/vue-3'
import MathematicsDialog from './MathematicsDialog.vue'
import bus from '@/bus'
const props = defineProps<{ editor: Editor }>()
const isActive = computed(() => {
  return props.editor?.isActive('blockMath')
})
const dialogRef = ref<InstanceType<typeof MathematicsDialog>>()
const activeState = computed(() => {
  return isActive.value ? 'on' : 'off'
})

const focus = () => {
  dialogRef.value?.open(props.editor)
}
onMounted(() => {
  bus.on('edit-mathematics-block', (v: string) => {
    dialogRef.value?.open(props.editor, v)
  })
})
</script>
<style lang="scss" scoped></style>
