<template>
  <div style="height: 100%; display: flex; flex-direction: column" class="pr-4 pl-4">
    <div style="flex: 1; display: grid; grid-template-rows: auto 1fr">
      <div class="mb-8" v-for="qa in conversationRecordList" :key="qa.conversationRecordId">
        <!-- question -->
        <div class="flex w-full justify-end mb-2">
          <el-card style="--el-card-padding: 8px"> {{ qa.question.question }} </el-card>
        </div>
        <!-- 响应 -->
        <div class="flex w-full">
          <el-avatar src="/ui/user.jpeg" class="mr-4" />
          <div style="width: calc(100% - 110px)">
            <el-card
              style="--el-card-padding: 0px 12px 0 12px"
              v-for="key in Object.keys(qa.answer)"
              :key="key"
            >
              <template v-for="node_id in Object.keys(qa.answer[key])" :key="node_id">
                <Answer
                  :chunk="qa.answer[key][node_id][display]"
                  :type="display"
                  v-for="display in Object.keys(qa.answer[key][node_id])"
                  :key="display"
                >
                </Answer>
              </template>
            </el-card>
          </div>
        </div>
      </div>
    </div>
    <div class="sticky bottom-0 left-0 z-99999 right-0 pb-4 bg-white">
      <div style="background-color: rgb(243, 244, 246)" class="p-4 rounded-2xl">
        <el-input
          v-model="question"
          class="no-border-input custom-textarea"
          :autosize="{ minRows: 1, maxRows: 6 }"
          type="textarea"
          autosizee
        >
        </el-input>
        <div class="w-full h-4 relative">
          <div class="absolute right-1 cursor-pointer">
            <el-icon @click="conversation(question)"><Promotion /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { inject, reactive, ref } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { ConversationStream } from '@/api/common'
import Answer from '@/components/Answer/index.vue'

const conversationRecordList = ref<Array<any>>([])
const question = ref<string>('')
const conversationAPI = inject('conversationAPI') as any
const displayDict: any = {
  reasoning_content: 'reasoning',
  content: 'markdown'
}

const getOnStream = (conversationRecord: any) => {
  const onStream = (chunk: any) => {
    if (!conversationRecord['conversationRecordId'] && chunk.conversationRecordId) {
      conversationRecord.conversationRecordId = chunk.conversationRecordId
    }
    if (!conversationRecord.answer[chunk.display_id]) {
      conversationRecord.answer[chunk.display_id] = {}
    }
    if (!conversationRecord.answer[chunk.display_id][chunk.node_id]) {
      conversationRecord.answer[chunk.display_id][chunk.node_id] = {}
    }
    Object.keys(displayDict).forEach((key) => {
      if (chunk[key]) {
        if (conversationRecord.answer[chunk.display_id][chunk.node_id]) {
          if (conversationRecord.answer[chunk.display_id][chunk.node_id][displayDict[key]]) {
            conversationRecord.answer[chunk.display_id][chunk.node_id][displayDict[key]][
              displayDict[key]
            ] += chunk[key]
          } else {
            conversationRecord.answer[chunk.display_id][chunk.node_id][displayDict[key]] = {
              ...chunk,
              [displayDict[key]]: chunk[key]
            }
          }
        } else {
          conversationRecord.answer[chunk.display_id][chunk.node_id][displayDict[key]] = {
            ...chunk,
            [displayDict[key]]: chunk[key]
          }
        }
      }
      return null
    })
  }
  return onStream
}

const conversation = (text: string) => {
  question.value = ''
  const conversationRecord = reactive({
    answer: {},
    question: { question: text }
  })
  conversationRecordList.value.push(conversationRecord)
  new ConversationStream(
    conversationAPI(text),
    getOnStream(conversationRecord),
    () => {},
    () => {}
  ).stream()
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
