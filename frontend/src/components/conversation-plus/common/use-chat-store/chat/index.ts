import { ref, computed, reactive } from 'vue'
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

// ─── 当前消息列表 ─────────────────────────────────────────────────
const messages = ref<Msg[]>([])

// ─── 消息分页状态：conversationId 隔离 ─────────────────────────────
const msgCurrentPageMap = reactive(new Map<string, number>())
const msgTotalMap = reactive(new Map<string, number>())
const msgLoadingMap = reactive(new Map<string, boolean>())
const MSG_PAGE_SIZE = 2

const getMsgCurrentPage = (cid: string) => msgCurrentPageMap.get(cid) ?? 1
const setMsgCurrentPage = (cid: string, page: number) => {
  msgCurrentPageMap.set(cid, page)
}

const getMsgTotal = (cid: string) => msgTotalMap.get(cid) ?? 0
const setMsgTotal = (cid: string, total: number) => {
  msgTotalMap.set(cid, total)
}

const isMsgLoading = (cid: string) => msgLoadingMap.get(cid) === true
const setMsgLoading = (cid: string, loading: boolean) => {
  msgLoadingMap.set(cid, loading)
}

const resetMsgState = (cid: string) => {
  msgCurrentPageMap.set(cid, 1)
  msgTotalMap.set(cid, 0)
  msgLoadingMap.set(cid, false)
}

const clearMsgState = (cid: string) => {
  msgCurrentPageMap.delete(cid)
  msgTotalMap.delete(cid)
  msgLoadingMap.delete(cid)
}

// ─── 流式断点状态：conversationId -> lastIndex ─────────────────────
const streamIndexMap = reactive(new Map<string, number>())

const streamIndexStorageKey = (cid: string) => `stream:index:${cid}`

const getStoredStreamIndex = (cid: string) => {
  const memoryIndex = streamIndexMap.get(cid)
  if (typeof memoryIndex === 'number') {
    return memoryIndex
  }

  const value = sessionStorage.getItem(streamIndexStorageKey(cid))
  if (!value) return 0

  const index = Number(value)
  return Number.isFinite(index) ? index : 0
}

const setStoredStreamIndex = (cid: string, index: number) => {
  streamIndexMap.set(cid, index)
  sessionStorage.setItem(streamIndexStorageKey(cid), String(index))
}

const clearStoredStreamIndex = (cid: string) => {
  streamIndexMap.delete(cid)
  sessionStorage.removeItem(streamIndexStorageKey(cid))
}

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

  const current = computed(() => chats.value.find((c) => c.id === conversationId.value))

  const getCurrentConversationId = () => {
    const cid = conversationId.value ?? current.value?.id

    if (!cid) {
      throw new Error('conversationId is empty')
    }

    return cid
  }

  const hasMore = computed(() => chats.value.length < total.value)

  const msgLoading = computed(() => {
    const cid = conversationId.value ?? current.value?.id
    return cid ? isMsgLoading(cid) : false
  })

  const hasMoreMsg = computed(() => {
    const cid = conversationId.value ?? current.value?.id
    if (!cid) return false

    return messages.value.length < getMsgTotal(cid)
  })

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

  // ─── 路由跳转 ───────────────────────────────────────────────────
  const toConversation = (cid: string) => {
    return router.push({
      name: 'conversation',
      params: {
        applicationId: applicationId.value,
        conversationId: cid
      }
    })
  }

  const toNewConversation = () => {
    messages.value = []

    router.push({
      name: 'conversation-new'
    })
  }

  // ─── 流式恢复接口 ────────────────────────────────────────────────

  /**
   * 查询当前会话是否还有流。
   *
   * 返回示例：
   * { code: 200, data: { status: true } }
   */
  const statusStream = () => {
    const cid = getCurrentConversationId()
    return conversationAPI.statusStream(cid)
  }

  /**
   * 恢复当前会话流。
   *
   * index 从 store 内部根据 conversationId 获取。
   */
  const resumeStream = () => {
    const cid = getCurrentConversationId()
    const index = getStoredStreamIndex(cid)

    return conversationAPI.resumeStream(cid, index)
  }

  const getStreamIndex = () => {
    const cid = getCurrentConversationId()
    return getStoredStreamIndex(cid)
  }

  const setStreamIndex = (index: number) => {
    const cid = getCurrentConversationId()
    setStoredStreamIndex(cid, index)
  }

  const clearStreamIndex = () => {
    const cid = getCurrentConversationId()
    clearStoredStreamIndex(cid)
  }

  // ─── 会话分页 ───────────────────────────────────────────────────
  const pageConversation = async (
    query: Omit<QueryConversationVO, 'currentPage' | 'pageSize'>,
    loading?: Ref<boolean>
  ) => {
    currentPage.value = 1

    const res = await conversationAPI.pageConversation(
      {
        ...query,
        currentPage: 1,
        pageSize: PAGE_SIZE
      },
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

  // ─── 消息分页 ───────────────────────────────────────────────────
  const loadMessages = async (cid: string, reset = true) => {
    if (isMsgLoading(cid)) return

    if (reset) {
      messages.value = []
      resetMsgState(cid)
    }

    setMsgLoading(cid, true)

    try {
      const res = await conversationAPI.pageConversationMessage(cid, {
        currentPage: getMsgCurrentPage(cid),
        pageSize: MSG_PAGE_SIZE
      })

      setMsgTotal(cid, res.data.total)

      const records = [...res.data.records].reverse() as Msg[]

      /**
       * 防止快速切换会话时，旧会话请求回来覆盖当前消息列表。
       */
      if (cid !== conversationId.value) {
        return
      }

      if (reset) {
        messages.value = records
      } else {
        messages.value = [...records, ...messages.value]
      }
    } finally {
      setMsgLoading(cid, false)
    }
  }

  const loadMoreMessages = async () => {
    const cid = conversationId.value ?? current.value?.id
    if (!cid) return

    if (!hasMoreMsg.value || isMsgLoading(cid)) return

    const oldPage = getMsgCurrentPage(cid)
    setMsgCurrentPage(cid, oldPage + 1)

    try {
      await loadMessages(cid, false)
    } catch (e) {
      setMsgCurrentPage(cid, oldPage)
      throw e
    }
  }

  // ─── 会话 CRUD ─────────────────────────────────────────────────
  const newChat = async (name?: string) => {
    const res = await conversationAPI.createConversation(name ?? t('conversation.newChat'))

    chats.value.unshift(res.data)
    total.value++

    await toConversation(res.data.id)

    messages.value = []
    resetMsgState(res.data.id)
    clearStoredStreamIndex(res.data.id)

    return res.data
  }

  /**
   * 这里只切换路由，不主动 loadMessages。
   *
   * 对话主组件 watch current.id 后，会统一：
   * 1. loadMessages
   * 2. statusStream
   * 3. resumeStream
   */
  const switchChat = async (id: string) => {
    await toConversation(id)
  }

  const deleteChat = async (id: string) => {
    await conversationAPI.delConversation(id)

    const idx = chats.value.findIndex((c) => c.id === id)
    if (idx < 0) return

    clearMsgState(id)
    clearStoredStreamIndex(id)

    chats.value.splice(idx, 1)
    total.value = Math.max(0, total.value - 1)

    if (!chats.value.length) {
      messages.value = []

      router.push({
        name: 'conversation-new'
      })

      return
    }

    if (conversationId.value === id) {
      const next = chats.value[idx] ?? chats.value[idx - 1]

      if (next?.id) {
        await switchChat(next.id)
      }
    }
  }

  const renameChat = (id: string, name: string) => {
    const trimmed = name.trim()
    if (!trimmed) return

    conversationAPI.modifyName(id, trimmed).then(() => {
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

  const init = async (query?: QueryConversationVO, loading?: Ref<boolean>) => {
    if (!query) {
      query = {
        currentPage: 1,
        pageSize: PAGE_SIZE
      }
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

    pushMessage,

    statusStream,
    resumeStream,
    getStreamIndex,
    setStreamIndex,
    clearStreamIndex
  }
}
