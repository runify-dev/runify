<template>
  <div style="height: 100%; display: flex; flex-direction: column" class="pr-4 pl-4">
    <div style="flex: 1; display: grid; grid-template-rows: auto 1fr">
      <div class="mb-8 overflow-hidden" v-for="(message, index) in messages" :key="index">
        <Contents :content="message.content"></Contents>
      </div>
    </div>
    <div class="sticky bottom-0 left-0 z-99999 right-0 pb-4 bg-white">
      <div style="background-color: rgb(243, 244, 246)" class="p-4 rounded-2xl">
        <el-input
          v-model="question.content"
          class="no-border-input custom-textarea"
          :autosize="{ minRows: 1, maxRows: 6 }"
          type="textarea"
          @keydown.enter.exact.stop.prevent="conversation(question)"
          autosizee
        >
        </el-input>
        <div class="w-full h-4 relative">
          <div class="absolute right-1 cursor-pointer">
            <el-button :disabled="isConversation" link>
              <el-icon @click="conversation(question)"><Promotion /></el-icon
            ></el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, inject, reactive, ref } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { ConversationStream } from '@/api/common'
import Contents from './content/Contents.vue'

const messages = ref<Array<any>>([])
const emit = defineEmits(['chanage'])
const question = ref<any>({
  content: ''
})
const isConversation = computed(() => {
  return !question.value.content.trim()
})
const conversationAPI = inject('conversationAPI') as any

const getOnStream = (message: any) => {
  const index: any[] = []
  const onStream = (chunk: any) => {
    chunk.content.forEach((content: any) => {
      const id = content.realNodeId + '_' + content.type
      let i = index.indexOf(id)
      if (i < 0) {
        i = index.length
        index.push(id)
      }
      if (message.content.length <= i) {
        message.content[i] = content
      } else {
        message.content[i].content += content.content
      }
    })
    emit('chanage')
  }
  return onStream
}

const conversation = (q: any) => {
  if (isConversation.value) {
    return
  }

  messages.value.push({
    type: 'USER',
    content: [{ ...q, type: 'QUESTION' }]
  })
  const answerMessage = reactive({
    type: 'LOADING',
    content: []
  })

  messages.value.push(answerMessage)
  new ConversationStream(
    conversationAPI({ ...q }),
    getOnStream(answerMessage),
    () => {},
    () => {}
  ).stream()
  question.value.content = ''
}
</script>
<style lang="scss">
.no-border-input .el-textarea__inner {
  box-shadow: none !important;
  resize: none; /* 禁用调整大小 */
  background-color: rgb(243, 244, 246);
}
.no-border-input .el-input__wrapper:hover {
  box-shadow: none !important;
}
.no-border-input .el-input__wrapper.is-focus {
  box-shadow: none !important;
}
</style>
