<template>
  <div
    class="absolute z-99999 bottom-8 right-6 bg-white rounded-xl overflow-hidden"
    style="height: 450px; width: 400px"
  >
    <Conversation></Conversation>
  </div>
</template>
<script setup lang="ts">
import { onMounted, provide, ref } from 'vue'
import applicationAPI from '@/api/application'
import { v4 as uuidv4 } from 'uuid'
import Conversation from '@/components/conversation-plus/index.vue'
import { Scroll } from '@/components/conversation/index'
import ConversationList from './ConversationList.vue'
const drawerOpen = ref<boolean>(false)
const ConversationListRef = ref<InstanceType<typeof ConversationList>>()
const emit = defineEmits(['close'])
const touchStartX = ref<number>(0)
const touchStartY = ref<number>(0)
const isSwiping = ref(false)
const swipeDirection = ref<string | null>(null)
const handleTouchStart = (e: any) => {
  touchStartX.value = e.touches[0].clientX
  touchStartY.value = e.touches[0].clientY
  isSwiping.value = true
  swipeDirection.value = null
}

const handleTouchMove = (e: any) => {
  if (!isSwiping.value) return

  const touchX = e.touches[0].clientX
  const touchY = e.touches[0].clientY

  const deltaX = touchX - touchStartX.value
  const deltaY = touchY - touchStartY.value

  // 确定滑动方向（水平或垂直）
  if (!swipeDirection.value) {
    if (Math.abs(deltaX) > Math.abs(deltaY)) {
      swipeDirection.value = 'horizontal'
    } else {
      swipeDirection.value = 'vertical'
    }
  }

  // 如果是水平滑动且从左侧边缘开始
  if (swipeDirection.value === 'horizontal' && touchStartX.value < 50) {
    e.preventDefault()

    // 向右滑动打开抽屉
    if (deltaX > 0 && !drawerOpen.value) {
      open()
    }
    // 向左滑动关闭抽屉
    else if (deltaX < 0 && drawerOpen.value) {
      close()
    }
  }
}

const handleTouchEnd = () => {
  isSwiping.value = false
  swipeDirection.value = null
}
const scrollRef = ref<any>()
const props = defineProps<{
  forderId: string
  application: any
}>()
const conversationId = ref<string>()
let scroll: any

const scrollBottom = () => {
  scroll.scrollBottom()
}

provide('conversationAPI', (question: any) => {
  if (!conversationId.value) {
    console.log(conversationId.value)
    return applicationAPI
      .createConversation(props.application.id, question.content)
      .then((ok) => {
        conversationId.value = ok.data.id
        return ok
      })
      .then((ok: any) => {
        return applicationAPI.chat(props.application.id, ok.data.id, {
          content: question,
          workflowRunId: uuidv4()
        })
      })
  } else {
    return applicationAPI.chat(props.application.id, conversationId.value, {
      content: question,
      workflowRunId: uuidv4()
    })
  }
})
onMounted(() => {
  const element = scrollRef.value
})
</script>
<style lang="scss"></style>
