<template>
  <div class="absolute z-99999 bottom-8 right-6 bg-white rounded-xl overflow-hidden">
    <div
      class="flex justify-between content-center items-center"
      style="
        background: linear-gradient(135deg, rgba(29, 43, 100, 0.79), rgb(248, 205, 218));
        height: 50px;
      "
    >
      <div class="pl-4">
        {{ application.name }}
      </div>
      <div class="pr-4">
        <el-icon @click="emit('close')" class="cursor-pointer"><Close /></el-icon>
      </div>
    </div>

    <div ref="scrollRef" class="overflow-auto pt-4" style="height: 520px; width: 450px">
      <Conversation @chanage="scrollBottom"></Conversation>
    </div>
  </div>
</template>
<script setup lang="ts">
import Conversation from '@/components/conversation/index.vue'
import { onMounted, provide, ref } from 'vue'
import applicationAPI from '@/api/application'
import { v4 as uuidv4 } from 'uuid'
import { Scroll } from '@/components/conversation/index'
const emit = defineEmits(['close'])
const scrollRef = ref<any>()
const props = defineProps<{
  forderId: string
  application: any
}>()
const conversationId = ref<string>()
let scroll: any
const getConversationId = () => {
  if (!conversationId.value) {
    conversationId.value = uuidv4()
  }
  return conversationId.value
}
const scrollBottom = () => {
  scroll.scrollBottom()
}

provide('conversationAPI', (question: any) => {
  return applicationAPI.chat(props.forderId, props.application.id, {
    question: question,
    conversationId: getConversationId()
  })
})
onMounted(() => {
  const element = scrollRef.value
  scroll = new Scroll(element)
})
</script>
<style lang="scss"></style>
