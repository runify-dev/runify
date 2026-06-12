import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import type { Conversation, FlatLabel, FlatItem, FlatRow } from '../../types'
import { formatDateTime } from '@/utils/common'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import conversationAPI from '@/api/conversation'
import applicationAPI from '@/api/application'
import type { QueryConversationVO } from '@/api/type/conversation'
import { t } from '@/locales'
import { useStreamManager } from '../shared/use-stream-manager'
import { useMessagePagination } from '../shared/use-message-pagination'
import { useConversationCrud } from '../shared/use-conversation-crud'

// ─── 单例：当前会话 ID（sidebar 和 chat-panel 共享）──────────────
const conversationId = ref<string | undefined>()

// ─── 流式断点持久化 ──────────────────────────────────────────────
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

export function useChatStore() {
  const route = useRoute()
  const applicationId = computed(() => route.params.id as string)

  // ─── 应用信息 ─────────────────────────────────────────────────
  const appInfo = ref<{ name: string; icon: string } | null>(null)

  const fetchAppInfo = async () => {
    try {
      const res = await applicationAPI.getApplicationInfo(applicationId.value)
      appInfo.value = { name: res.data.name || '', icon: res.data.icon || '' }
    } catch {}
  }

  // ─── 会话 CRUD ─────────────────────────────────────────────────
  const { chats, hasMore, loadingMore, pageConversation, loadMore, total } = useConversationCrud({
    pageConversationAPI: (query: any, loading?: Ref<boolean>) => {
      return applicationAPI.pageConversation(
        applicationId.value,
        query.currentPage,
        query.pageSize,
        { ...query, executeType: 'DEBUG' },
        loading
      )
    }
  })

  const current = computed(() => chats.value.find((c) => c.id === conversationId.value))

  // ─── 消息分页 ─────────────────────────────────────────────────
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

  // ─── 消息推送（含会话名称更新）──────────────────────────────────
  const wrappedPushMessage = (msg: any) => {
    pushMessage(msg)

    if (messages.value.length === 1 && current.value) {
      const question =
        (msg.content as any[]).find((item: any) => item.type === 'QUESTION')?.content ?? ''
      current.value.name = question.length > 24 ? question.slice(0, 24) + '…' : question
      current.value.createTime = formatDateTime()
    }
  }

  // ─── Stream 管理 ──────────────────────────────────────────────
  const streamManager = useStreamManager()

  const streamLoading = computed(() => {
    const cid = conversationId.value
    return cid ? streamManager.getStreamLoading(cid) : false
  })

  // ─── 分组 ─────────────────────────────────────────────────────
  const GROUP_I18N_KEY = {
    today: 'conversation.group.today',
    yesterday: 'conversation.group.yesterday',
    thisWeek: 'conversation.group.thisWeek',
    earlier: 'conversation.group.earlier'
  } as const

  const flatItems = computed<FlatRow[]>(() => {
    const now = dayjs()
    const buckets: Record<string, Conversation[]> = {
      today: [],
      yesterday: [],
      thisWeek: [],
      earlier: []
    }

    ;[...chats.value]
      .sort((a, b) => dayjs(b.createTime).valueOf() - dayjs(a.createTime).valueOf())
      .forEach((c) => {
        const time = dayjs(c.createTime)
        if (time.isSame(now, 'day')) buckets['today'].push(c)
        else if (time.isSame(now.subtract(1, 'day'), 'day')) buckets['yesterday'].push(c)
        else if (time.isAfter(now.subtract(7, 'day'))) buckets['thisWeek'].push(c)
        else buckets['earlier'].push(c)
      })

    return Object.entries(buckets)
      .filter(([, v]) => v.length)
      .flatMap(([key, items]) => [
        { type: 'label', label: t(GROUP_I18N_KEY[key as keyof typeof GROUP_I18N_KEY]) } as FlatLabel,
        ...items.map((c) => ({ type: 'item', ...c }) as FlatItem)
      ])
  })

  // ─── 会话操作 ─────────────────────────────────────────────────
  const newChat = async (name?: string) => {
    const chat = await applicationAPI.createConversation(
      applicationId.value,
      name&&name?.length>0 ? name: t('conversation.newChat')
    )

    chats.value.unshift(chat.data)
    conversationId.value = chat.data.id

    messages.value = []
    resetMsgState(chat.data.id)
    clearStoredStreamIndex(chat.data.id)

    return chat.data
  }

  const toNewConversation = () => {
    messages.value = []
    return newChat(t('conversation.newChat'))
  }

  const switchChat = (id: string) => {
    conversationId.value = id
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

    applicationAPI.modifyName(applicationId.value, id, trimmed).then(() => {
      const c = chats.value.find((x) => x.id === id)
      if (c) c.name = trimmed
    })
  }

  const chat = (q: any) => {
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    return applicationAPI.chat(applicationId.value, cid, q)
  }

  // ─── 流式操作 ─────────────────────────────────────────────────
  const statusStream = () => {
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    return applicationAPI.statusStream(applicationId.value, cid)
  }
  const cancel=()=>{
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

  // ─── 初始化 ───────────────────────────────────────────────────
  const init = async (
    query?: Omit<QueryConversationVO, 'currentPage' | 'pageSize'>,
    loading?: Ref<boolean>
  ) => {
    if (!query) {
      query = { currentPage: 1, pageSize: 30 }
    }

    fetchAppInfo()
    await pageConversation(query, loading)
  }

  return {
    // 状态
    appInfo,
    chats,
    messages,
    conversationId,
    applicationId,
    current,
    flatItems,
    showBack: ref(false),
    goBack: () => {},

    // 分页
    hasMore,
    hasMoreMsg: computed(() => hasMoreMsg(conversationId.value)),
    loadingMore,
    msgLoading: computed(() => msgLoading(conversationId.value)),
    streamLoading,

    // 会话操作
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

    // 流式操作
    chat,
    statusStream,
    resumeStream,
    setStreamIndex,
    clearStreamIndex,
    cancel,
    // stream manager
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
