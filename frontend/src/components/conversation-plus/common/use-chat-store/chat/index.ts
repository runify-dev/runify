import { ref, reactive, computed, inject } from 'vue'
import type { Conversation, Msg, ConversationGroup } from './types'
import { formatDateTime } from "@/utils/common"
import dayjs from 'dayjs'
const createConversation = inject('createConversation') as any

let uid = 1

// 单例：所有组件共享同一份状态
const chats = reactive<Conversation[]>([
])


const activeId = ref(1)

export function useChatStore() {
  const current = computed(() => chats.find(c => c.id === activeId.value) ?? chats[0])
  const messages = reactive<Msg[]>([]);
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
    createConversation(name ? name : '新建对话').then((ok: any) => {
      chats.unshift(ok.data)
      activeId.value = c.id
    })


  }

  const switchChat = (id: number) => {
    activeId.value = id
  }

  const deleteChat = (id: number) => {
    const i = chats.findIndex(c => c.id === id)
    if (i < 0) return
    chats.splice(i, 1)
    if (!chats.length) { newChat(); return }
    if (activeId.value === id) activeId.value = chats[0].id
  }

  const renameChat = (id: number, title: string) => {
    const c = chats.find(x => x.id === id)

  }


  const conversation = (msg: Msg) => {
    messages?.push(msg)
    if (messages && messages.length === 1) {
      current.value.title = msg.content.slice(0, 24) + (msg.content.length > 24 ? '…' : '')
      current.value.ts = new Date()
    }
  }

  return {
    chats,
    activeId,
    current,
    grouped,
    newChat,
    switchChat,
    deleteChat,
    renameChat,
    conversation,
  }
}
