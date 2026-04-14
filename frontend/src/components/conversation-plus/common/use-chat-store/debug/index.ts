import { ref, reactive, computed } from 'vue'
import type { Ref } from 'vue'
import type { Conversation, Msg } from '../../types'
import { formatDateTime } from '@/utils/common'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import conversationAPI from '@/api/conversation'
import applicationAPI from "@/api/application"
import type { QueryConversationVO } from '@/api/type/conversation'
import { t } from '@/locales'
// ─── 类型 ─────────────────────────────────────────────────────────
export type FlatLabel = { type: 'label'; label: string }
export type FlatItem = { type: 'item' } & Conversation
export type FlatRow = FlatLabel | FlatItem

export type ConversationGroup = { label: string; items: Conversation[] }

// ─── 单例状态 ─────────────────────────────────────────────────────
const chats = reactive<Conversation[]>([])
const conversationId = ref<string | undefined>()
const messages = reactive<Msg[]>([])

// ─── 会话分页状态 ─────────────────────────────────────────────────
const currentPage = ref(1)
const total = ref(0)
const loadingMore = ref(false)
const PAGE_SIZE = 30

// ─── 消息分页状态 ─────────────────────────────────────────────────
const msgCurrentPage = ref(1)
const msgTotal = ref(0)
const msgLoading = ref(false)
const MSG_PAGE_SIZE = 2

export function useChatStore() {

  const route = useRoute()
  const applicationId = computed(() => route.params.id as string)
  const pageConversationProxy = (query: any, loading?: Ref<boolean>) => {
    return applicationAPI.pageConversation(applicationId.value, query.currentPage, query.pageSize, { ...query, executeType: "DEBUG" }, loading)
  }
  const pageConversationMessageProxy = (conversationId: string,
    query: any,
    loading?: Ref<boolean>) => {
    return applicationAPI.pageConversationMessage(applicationId.value, conversationId, query.currentPage, query.pageSize, query, loading)
  }
  const current = computed(() => chats.find((c) => c.id === conversationId.value))

  const hasMore = computed(() => chats.length < total.value)
  const hasMoreMsg = computed(() => messages.length < msgTotal.value)

  // ─── 分组（今天/昨天/本周/更早）────────────────────────────────
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
        .forEach((c) => {
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

  const toNewConversation = () => {
    messages.splice(0)
    msgCurrentPage.value = 1
    msgTotal.value = 0
    return newChat("新建对话")
  }
  const flatItems = computed<FlatRow[]>(() =>
    grouped.value.flatMap(({ label, items }) => [
      { type: 'label', label } as FlatLabel,
      ...items.map((c) => ({ type: 'item', ...c }) as FlatItem)
    ])
  )

  // ─── 会话分页 ─────────────────────────────────────────────────
  const pageConversation = async (
    query: Omit<QueryConversationVO, 'currentPage' | 'pageSize'>,
    loading?: Ref<boolean>
  ) => {
    currentPage.value = 1
    const res = await pageConversationProxy(
      { ...query, currentPage: 1, pageSize: PAGE_SIZE },
      loading
    )
    chats.splice(0, chats.length, ...res.data.records)
    total.value = res.data.total
  }

  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
    try {
      const res = await pageConversationProxy({
        currentPage: currentPage.value + 1,
        pageSize: PAGE_SIZE
      })
      chats.push(...res.data.records)
      total.value = res.data.total
      currentPage.value += 1
    } finally {
      loadingMore.value = false
    }
  }

  // ─── 消息分页 ─────────────────────────────────────────────────
  const loadMessages = async (cid: string, reset = true) => {
    if (msgLoading.value) return
    if (reset) {
      messages.splice(0)
      msgCurrentPage.value = 1
      msgTotal.value = 0
    }
    msgLoading.value = true
    try {
      const res = await pageConversationMessageProxy(cid, {
        currentPage: msgCurrentPage.value,
        pageSize: MSG_PAGE_SIZE
      })
      msgTotal.value = res.data.total
      const records = [...res.data.records].reverse() as Msg[]
      if (reset) {
        messages.splice(0, messages.length, ...records)
      } else {
        messages.splice(0, 0, ...records)
      }
    } finally {
      msgLoading.value = false
    }
  }

  const loadMoreMessages = async () => {
    if (!hasMoreMsg.value || msgLoading.value) return
    const cid = conversationId.value ?? current.value?.id
    if (!cid) return
    msgCurrentPage.value += 1
    await loadMessages(cid, false)
  }

  // ─── 会话 CRUD ────────────────────────────────────────────────
  const newChat = async (name?: string) => {
    const chat = await applicationAPI.createConversation(applicationId.value, name ?? t('conversation.newChat'))
    chats.unshift(chat.data)
    conversationId.value = chat.data.id
    messages.splice(0)
    msgCurrentPage.value = 1
    msgTotal.value = 0
    return Promise.resolve(chat.data)
  }

  const switchChat = (id: string) => {
    conversationId.value = id
    return loadMessages(id)
  }

  const deleteChat = (id: string) => {
    const i = chats.findIndex((c) => c.id === id)
    if (i < 0) return
    chats.splice(i, 1)
    total.value = Math.max(0, total.value - 1)
    if (!chats.length) {
      messages.splice(0)
      msgCurrentPage.value = 1
      msgTotal.value = 0
      conversationId.value = undefined
      return
    }
    if (conversationId.value === id) {
      const next = chats[i] ?? chats[i - 1]
      if (next?.id) switchChat(next.id)
    }
  }

  const renameChat = (id: string, name: string) => {
    const trimmed = name.trim()
    if (!trimmed) return
    conversationAPI.modifyName(id, trimmed).then(() => {
      const c = chats.find((x) => x.id === id)
      if (c) c.name = trimmed
    })
  }

  const pushMessage = (msg: Msg) => {
    messages.push(msg)
    if (messages.length === 1 && current.value) {
      const content = (msg.content as any[]).find((item) => item.type === 'QUESTION')?.content ?? ''
      current.value.name = content.length > 24 ? content.slice(0, 24) + '…' : content
      current.value.createTime = formatDateTime()
    }
  }

  const init = async (
    query?: Omit<QueryConversationVO, 'currentPage' | 'pageSize'>,
    loading?: Ref<boolean>
  ) => {
    if (!query) {
      query = { currentPage: 1, pageSize: PAGE_SIZE }
    }
    await pageConversation(query, loading)
    if (conversationId.value) {
      await loadMessages(conversationId.value)
    }
  }

  return {
    toNewConversation,
    chats,
    messages,
    conversationId,
    applicationId,
    current,
    grouped,
    flatItems,
    hasMore,
    hasMoreMsg,
    loadingMore,
    msgLoading,
    init,
    pageConversation,
    loadMore,
    loadMessages,
    loadMoreMessages,
    newChat,
    switchChat,
    deleteChat,
    renameChat,
    pushMessage
  }
}
