import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import type { Conversation, FlatLabel, FlatItem, FlatRow } from '../../types'
import { formatDateTime } from '@/utils/common'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import applicationAPI from '@/api/application'
import type { QueryConversationVO } from '@/api/type/conversation'
import { t } from '@/locales'
import { useStreamManager } from '../shared/use-stream-manager'
import { useMessagePagination } from '../shared/use-message-pagination'

const conversationId = ref<string | undefined>()

const streamIndexMap = new Map<string, number>()
const streamIndexStorageKey = (cid: string) => `stream:index:${cid}`

const getStoredStreamIndex = (cid: string) => {
  const memoryIndex = streamIndexMap.get(cid)
  if (typeof memoryIndex === 'number') return memoryIndex
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

// 单例会话列表
const chats = ref<Conversation[]>([])
const currentPage = ref(1)
const total = ref(0)
const loadingMore = ref(false)

export function useChatStore() {
  const route = useRoute()
  const router = useRouter()
  const applicationId = computed(() => (route.params.applicationId || route.params.id) as string)

  const appInfo = ref<{ name: string; icon: string } | null>(null)

  const fetchAppInfo = async () => {
    try {
      const res = await applicationAPI.getApplicationInfo(applicationId.value)
      appInfo.value = { name: res.data.name || '', icon: res.data.icon || '' }
    } catch {}
  }

  const hasMore = computed(() => chats.value.length < total.value)

  const pageConversation = async (query?: any, loading?: Ref<boolean>) => {
    currentPage.value = 1
    const res = await applicationAPI.mineConversation(
      applicationId.value,
      query?.currentPage ?? 1,
      query?.pageSize ?? 30,
      loading
    )
    chats.value = res.data.records
    total.value = res.data.total
  }

  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return
    loadingMore.value = true
    try {
      const res = await applicationAPI.mineConversation(
        applicationId.value,
        currentPage.value + 1,
        30
      )
      chats.value.push(...res.data.records)
      total.value = res.data.total
      currentPage.value += 1
    } finally {
      loadingMore.value = false
    }
  }

  const current = computed(() => chats.value.find((c) => c.id === conversationId.value))

  const {
    messages,
    msgLoading,
    hasMoreMsg,
    loadMessages,
    loadMoreMessages,
    pushMessage,
    resetMsgState,
    clearMsgState
  } = useMessagePagination({
    pageConversationMessage: (cid: string, query: any) => {
      return applicationAPI.pageConversationMessage(
        applicationId.value,
        cid,
        query.currentPage,
        query.pageSize,
        query
      )
    },
    getCurrentName: () => current.value?.name,
    getCurrentCreateTime: () => current.value?.createTime
  })

  const wrappedPushMessage = (msg: any) => {
    pushMessage(msg)
    if (messages.value.length === 1 && current.value) {
      const question =
        (msg.content as any[]).find((item: any) => item.type === 'QUESTION')?.content ?? ''
      current.value.name = question.length > 24 ? question.slice(0, 24) + '…' : question
      current.value.createTime = formatDateTime()
    }
  }

  const streamManager = useStreamManager()

  const streamLoading = computed(() => {
    const cid = conversationId.value
    return cid ? streamManager.getStreamLoading(cid) : false
  })

  const flatItems = computed<FlatRow[]>(() => {
    const now = dayjs()
    const buckets: Record<string, Conversation[]> = {
      今天: [],
      昨天: [],
      本周: [],
      更早: []
    }
    ;[...chats.value]
      .sort((a, b) => dayjs(b.createTime).valueOf() - dayjs(a.createTime).valueOf())
      .forEach((c) => {
        const time = dayjs(c.createTime)
        if (time.isSame(now, 'day')) buckets['今天'].push(c)
        else if (time.isSame(now.subtract(1, 'day'), 'day')) buckets['昨天'].push(c)
        else if (time.isAfter(now.subtract(7, 'day'))) buckets['本周'].push(c)
        else buckets['更早'].push(c)
      })
    return Object.entries(buckets)
      .filter(([, v]) => v.length)
      .flatMap(([label, items]) => [
        { type: 'label', label } as FlatLabel,
        ...items.map((c) => ({ type: 'item', ...c }) as FlatItem)
      ])
  })

  const newChat = async (name?: string) => {
    const chat = await applicationAPI.createConversation(
      applicationId.value,
      name && name.length > 0 ? name : t('conversation.newChat'),
      'CONVERSATION'
    )
    chats.value.unshift(chat.data)
    conversationId.value = chat.data.id
    messages.value = []
    resetMsgState(chat.data.id)
    clearStoredStreamIndex(chat.data.id)
    router.push({ name: 'applicationChatConversation', params: { applicationId: applicationId.value, conversationId: chat.data.id } })
    return chat.data
  }

  const toNewConversation = () => {
    messages.value = []
    conversationId.value = undefined
    router.push({ name: 'applicationChat', params: { applicationId: applicationId.value } })
    return newChat('新建对话')
  }

  const switchChat = (id: string) => {
    conversationId.value = id
    router.push({ name: 'applicationChatConversation', params: { applicationId: applicationId.value, conversationId: id } })
  }

  const deleteChat = (id: string) => {
    const i = chats.value.findIndex((c) => c.id === id)
    if (i < 0) return
    clearStoredStreamIndex(id)
    clearMsgState(id)
    chats.value.splice(i, 1)
    if (!chats.value.length) {
      messages.value = []
      conversationId.value = undefined
      router.push({ name: 'applicationChat', params: { applicationId: applicationId.value } })
      return
    }
    if (conversationId.value === id) {
      const next = chats.value[i] ?? chats.value[i - 1]
      if (next?.id) switchChat(next.id)
    }
  }

  const renameChat = (id: string, name: string) => {
    const trimmed = name.trim()
    if (!trimmed) return
    // admin 模式下暂不支持重命名，可后续扩展
  }

  const chat = (q: any) => {
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    return applicationAPI.chat(applicationId.value, cid, q)
  }

  const statusStream = () => {
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    return applicationAPI.statusStream(applicationId.value, cid)
  }

  const cancel = () => {
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    return applicationAPI.cancel(applicationId.value, cid)
  }

  const resumeStream = () => {
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    const index = getStoredStreamIndex(cid)
    return applicationAPI.resumeStream(applicationId.value, cid, index)
  }

  const setStreamIndex = (index: number) => {
    const cid = conversationId.value
    if (cid) setStoredStreamIndex(cid, index)
  }

  const clearStreamIndex = () => {
    const cid = conversationId.value
    if (cid) clearStoredStreamIndex(cid)
  }

  const showBack = ref(true)

  const goBack = () => {
    router.push({ name: 'applicationOverview', params: { id: applicationId.value } })
  }

  const init = async (query?: any, loading?: Ref<boolean>) => {
    if (!query) {
      query = { currentPage: 1, pageSize: 30 }
    }
    fetchAppInfo()
    await pageConversation(query, loading)

    // 从路由恢复 conversationId
    const routeCid = route.params.conversationId as string
    if (routeCid) {
      conversationId.value = routeCid
    }
  }

  return {
    appInfo,
    chats,
    messages,
    conversationId,
    applicationId,
    current,
    flatItems,
    showBack,
    goBack,

    hasMore,
    hasMoreMsg: computed(() => hasMoreMsg(conversationId.value)),
    loadingMore,
    msgLoading: computed(() => msgLoading(conversationId.value)),
    streamLoading,

    init,
    pageConversation,
    loadMore,
    loadMessages,
    loadMoreMessages: () => loadMoreMessages(conversationId.value),
    newChat,
    switchChat,
    deleteChat,
    renameChat,
    pushMessage: wrappedPushMessage,
    toNewConversation,

    chat,
    statusStream,
    resumeStream,
    setStreamIndex,
    clearStreamIndex,
    cancel,
    startStream: streamManager.startStream,
    cancelStream: streamManager.cancelStream,
    switchConversation: (opts: { cid: string; getOnStream: () => (chunk: any) => void; onFinish?: () => void; onFailure?: () => void; skipLoadMessages?: boolean }) => {
      return streamManager.switchConversation({
        ...opts,
        loadMessages,
        statusStream,
        resumeStream
      })
    }
  }
}
