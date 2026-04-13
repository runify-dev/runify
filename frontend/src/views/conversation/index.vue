<template>
  <div class="h-full">
    <Conversation :defaultOpen="true" type="CONVERSATION"></Conversation>
  </div>
</template>
<script setup lang="ts">
import { provide } from 'vue'
import _conversationAPI from '@/api/conversation'
import { v4 as uuidv4 } from 'uuid'
import { useChatStore } from '@/components/conversation-plus/common/use-chat-store/index'
import Conversation from '@/components/conversation-plus/index.vue'
const { conversationId } = useChatStore('CONVERSATION')
provide('conversationAPI', (question: any) => {
  if (conversationId.value) {
    return _conversationAPI.conversation(conversationId.value, {
      content: question,
      workflowRunId: uuidv4()
    })
  }
  return Promise.reject()
})
</script>
<style lang="scss">
#app {
  height: 100dvh;
}
</style>
