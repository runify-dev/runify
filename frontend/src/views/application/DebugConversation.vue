<template>
  <div
    class="absolute z-99999 bottom-8 right-6 bg-white rounded-xl overflow-hidden"
    :style="toggleStyle"
  >
    <Conversation defaultMode="drawer" @close="$emit('close')" type="DEBUG">
      <template #header>
        <button class="hbtn" @click="toggle">
          <i class="pi pi-arrow-up-right-and-arrow-down-left-from-center"></i>
        </button>
        <button class="hbtn" @click="$emit('close')">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path
              d="M2 3h10M5 3V2h4v1M5.5 5v5M8.5 5v5M2.5 3l.5 9h8l.5-9"
              stroke="currentColor"
              stroke-width="1.3"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
      </template>
    </Conversation>
  </div>
</template>
<script setup lang="ts">
import { computed, provide, ref } from 'vue'
import applicationAPI from '@/api/application'
import { v4 as uuidv4 } from 'uuid'
import Conversation from '@/components/conversation-plus/index.vue'
import { useChatStore } from '@/components/conversation-plus/common/use-chat-store/index'
const { conversationId } = useChatStore('DEBUG')
defineEmits(['close'])
const expanded = ref<boolean>(false)
const toggle = () => {
  expanded.value = !expanded.value
}
const toggleStyle = computed(() => {
  return expanded.value
    ? {
        height: '100vh',
        width: '50%',
        bottom: 0,
        right: 0
      }
    : {
        height: '450px',
        width: '400px'
      }
})
const props = defineProps<{
  forderId: string
  application: any
}>()

provide('conversationAPI', (question: any) => {
  if (conversationId.value) {
    return applicationAPI.chat(props.application.id, conversationId.value, {
      content: question,
      workflowRunId: uuidv4()
    })
  }
})
</script>
<style lang="scss">
.hbtn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--t3);
  cursor: pointer;
  flex-shrink: 0;
  transition:
    background 0.12s,
    color 0.12s;
}
.hbtn:hover {
  background: var(--hv);
  color: var(--t1);
}
</style>
