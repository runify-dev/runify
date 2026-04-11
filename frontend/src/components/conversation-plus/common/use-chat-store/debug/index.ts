import { ref, reactive, computed, inject } from 'vue'
import type { Conversation, Msg, ConversationGroup } from '@/components/conversation-plus/common/types'
import { formatDateTime } from "@/utils/common"
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { v4 as uuidv4 } from 'uuid'
const createConversation = inject('createConversation') as any


// 单例：所有组件共享同一份状态
const chats = reactive<Conversation[]>([
])


const conversationId = ref<string>()
const messages = reactive<Msg[]>([])
export function useChatStore() {
  const route = useRoute();
  const applicationId: string = route.params.id as string
  const current = computed(() => chats.find(c => c.id === conversationId.value) ?? chats[0])

  const grouped = computed<ConversationGroup[]>(() => {
    const now = dayjs()
    const m: Record<string, Conversation[]> = {
      今天: [],
      昨天: [],
      本周: [],
      更早: []
    }

      ;[...chats]
        .sort((a, b) => dayjs(b.createTime).valueOf() - dayjs(a.createTime).valueOf())
        .forEach(c => {
          const time = dayjs(c.createTime)
          if (time.isSame(now, 'day')) {
            m['今天'].push(c)
          } else if (time.isSame(now.subtract(1, 'day'), 'day')) {
            m['昨天'].push(c)
          } else if (time.isAfter(now.subtract(7, 'day'))) {
            m['本周'].push(c)
          } else {
            m['更早'].push(c)
          }
        })

    return Object.entries(m)
      .filter(([, v]) => v.length)
      .map(([label, items]) => ({ label, items }))
  })

  const newChat = (name?: string) => {
    const chat: any = {
      id: undefined,
      applicationId: applicationId,
      name: name ? name : '新建对话',
      executeType: 'DEBUG',
      updateTime: formatDateTime(),
      createTime: formatDateTime()
    }
    if (!chats.some((chat: any) => chat.id == null)) {
      chats.unshift(chat)
    }
    return Promise.resolve(chat)

  }

  const switchChat = (id: number) => {
    conversationId.value = id
  }

  const deleteChat = (id: string) => {
    const i = chats.findIndex(c => c.id === id)
    if (i < 0) return
    chats.splice(i, 1)
    if (!chats.length) { newChat(); return }
    if (id === conversationId.value) conversationId.value = chats[0].id
  }

  const renameChat = (id: number, title: string) => {
    const c = chats.find(x => x.id === id)
  }


  const pushMessage = (msg: Msg) => {
    messages?.push(msg)
    if (messages && messages.length === 1) {
      const content = msg.content.find(item => item.type == 'QUESTION').content
      current.value.name = content.slice(0, 24) + (content.length > 24 ? '…' : '')
      current.value.createTime = formatDateTime()
    }
  }

  return {
    chats,
    messages,
    conversationId,
    current,
    grouped,
    newChat,
    switchChat,
    deleteChat,
    renameChat,
    pushMessage
  }
}
