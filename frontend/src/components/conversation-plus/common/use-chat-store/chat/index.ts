import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { Ref } from 'vue'
import type { Conversation, Msg, FlatLabel, FlatItem, FlatRow } from '../../types'
import { GroupKey } from '../../types'
import conversationAPI from '@/api/conversation'
import type { QueryConversationVO } from '@/api/type/conversation'
import dayjs from 'dayjs'
import { formatDateTime } from '@/utils/common'
import { t } from '@/locales'

const GROUP_I18N_KEY: Record<GroupKey, string> = {
  [GroupKey.Today]: 'conversation.group.today',
  [GroupKey.Yesterday]: 'conversation.group.yesterday',
  [GroupKey.ThisWeek]: 'conversation.group.thisWeek',
  [GroupKey.Earlier]: 'conversation.group.earlier'
}

const GROUPS = [GroupKey.Today, GroupKey.Yesterday, GroupKey.ThisWeek, GroupKey.Earlier] as const

// ─── 单例状态 ─────────────────────────────────────────────────────
const chats = ref<Conversation[]>([])
const currentPage = ref(1)
const total = ref(0)
const loadingMore = ref(false)
const PAGE_SIZE = 30

// ─── 消息分页状态 ─────────────────────────────────────────────────
const messages = ref<Msg[]>([])
const msgCurrentPage = ref(1)
const msgTotal = ref(0)
const msgLoading = ref(false)
const MSG_PAGE_SIZE = 2

function getGroupKey(createTime: string): GroupKey {
  const now = dayjs()
  const time = dayjs(createTime)
  if (time.isSame(now, 'day')) return GroupKey.Today
  if (time.isSame(now.subtract(1, 'day'), 'day')) return GroupKey.Yesterday
  if (time.isAfter(now.subtract(7, 'day'))) return GroupKey.ThisWeek
  return GroupKey.Earlier
}

export function useChatStore() {
  const route = useRoute()
  const router = useRouter()

  const conversationId = computed(() => route.params.conversationId as string | undefined)
  const applicationId = computed(() => route.params.applicationId as string)

  const hasMore = computed(() => chats.value.length < total.value)
  const hasMoreMsg = computed(() => messages.value.length < msgTotal.value)

  const current = computed(() => chats.value.find((c) => c.id === conversationId.value))

  const flatItems = computed<FlatRow[]>(() => {
    const buckets = Object.fromEntries(GROUPS.map((g) => [g, [] as Conversation[]])) as Record<
      GroupKey,
      Conversation[]
    >
      ;[...chats.value]
        .sort((a, b) => dayjs(b.createTime).valueOf() - dayjs(a.createTime).valueOf())
        .forEach((c) => buckets[getGroupKey(c.createTime)].push(c))

    return GROUPS.flatMap((key) => {
      const items = buckets[key]
      if (!items.length) return []
      return [
        { type: 'label', label: t(GROUP_I18N_KEY[key]) } as FlatLabel,
        ...items.map((c) => ({ type: 'item', ...c }) as FlatItem)
      ]
    })
  })

  const toConversation = (cid: string) =>
    router.push({
      name: 'conversation',
      params: { applicationId: applicationId.value, conversationId: cid }
    })

  const toNewConversation = () => {
    messages.value = []
    msgCurrentPage.value = 1
    msgTotal.value = 0
    router.push({ name: 'conversation-new' })
  }
  // ─── 会话分页 ─────────────────────────────────────────────────
  const pageConversation = async (
    query: Omit<QueryConversationVO, 'currentPage' | 'pageSize'>,
    loading?: Ref<boolean>
  ) => {
    currentPage.value = 1
    const res = await conversationAPI.pageConversation(
      { ...query, currentPage: 1, pageSize: PAGE_SIZE },
      loading
    )
    chats.value = res.data.records
    total.value = res.data.total
  }

  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
    try {
      const res = await conversationAPI.pageConversation({
        currentPage: currentPage.value + 1,
        pageSize: PAGE_SIZE
      })
      chats.value.push(...res.data.records)
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
      messages.value = []
      msgCurrentPage.value = 1
      msgTotal.value = 0
    }
    msgLoading.value = true
    try {
      const res = await conversationAPI.pageConversationMessage(cid, {
        currentPage: msgCurrentPage.value,
        pageSize: MSG_PAGE_SIZE
      })
      msgTotal.value = res.data.total
      // 后端倒序，reverse 后旧消息在上、新消息在下
      const records = [...res.data.records].reverse() as Msg[]
      if (reset) {
        messages.value = records
      } else {
        // 懒加载：更早的消息插到头部
        messages.value = [...records, ...messages.value]
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
    const res = await conversationAPI.createConversation(name ?? t('conversation.newChat'))
    chats.value.unshift(res.data)
    total.value++
    await toConversation(res.data.id)
    // 新会话清空消息
    messages.value = []
    msgCurrentPage.value = 1
    msgTotal.value = 0
    return res.data
  }

  const switchChat = async (id: string) => {
    await toConversation(id)
    await loadMessages(id)
  }

  const deleteChat = async (id: string) => {
    await conversationAPI.delConversation(id)
    const idx = chats.value.findIndex((c) => c.id === id)
    if (idx < 0) return
    chats.value.splice(idx, 1)
    total.value = Math.max(0, total.value - 1)
    if (!chats.value.length) {
      // 列表空了，清空消息回到空态即可
      messages.value = []
      msgCurrentPage.value = 1
      msgTotal.value = 0
      router.push({ name: 'conversation-new' })
      return
    }
    if (conversationId.value === id) {
      const next = chats.value[idx] ?? chats.value[idx - 1]
      if (next && next.id) await switchChat(next.id)
    }
  }

  const renameChat = (id: string, name: string) => {
    conversationAPI.modifyName(id, name.trim()).then((ok) => {
      const trimmed = name.trim()
      if (!trimmed) return
      const c = chats.value.find((x) => x.id === id)
      if (c) c.name = trimmed
    })
  }

  const pushMessage = (msg: Msg) => {
    messages.value.push(msg)
    if (messages.value.length === 1 && current.value) {
      const question =
        (msg.content as any[]).find((item) => item.type === 'QUESTION')?.content ?? ''
      current.value.name = question.length > 24 ? question.slice(0, 24) + '…' : question
      current.value.createTime = formatDateTime()
    }
  }

  const init = async (
    query?: QueryConversationVO,
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
