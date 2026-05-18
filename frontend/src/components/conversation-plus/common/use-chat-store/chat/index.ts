import {computed, reactive, ref} from 'vue'
import {formatDateTime} from '@/utils/common'
import {useRouter, useRoute} from 'vue-router'
import type {Ref} from 'vue'
import type {Conversation, FlatLabel, FlatItem, FlatRow} from '../../types'
import {GroupKey} from '../../types'
import conversationAPI from '@/api/conversation'
import type {QueryConversationVO} from '@/api/type/conversation'
import dayjs from 'dayjs'
import {t} from '@/locales'
import {useStreamManager} from '../shared/use-stream-manager'
import {useMessagePagination} from '../shared/use-message-pagination'
import {useConversationCrud} from '../shared/use-conversation-crud'
import useConversationTokenStore from '@/stores/converstaion/modules/conversation-token'

const GROUP_I18N_KEY: Record<GroupKey, string> = {
  [GroupKey.Today]: 'conversation.group.today',
  [GroupKey.Yesterday]: 'conversation.group.yesterday',
  [GroupKey.ThisWeek]: 'conversation.group.thisWeek',
  [GroupKey.Earlier]: 'conversation.group.earlier'
}

const GROUPS = [GroupKey.Today, GroupKey.Yesterday, GroupKey.ThisWeek, GroupKey.Earlier] as const

// ─── 流式断点持久化 ──────────────────────────────────────────────
const streamIndexMap = reactive(new Map<string, number>())

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
  const tokenStore = useConversationTokenStore()

  // ─── 登录守卫 ───────────────────────────────────────────────────
  const isLogged = computed(() => tokenStore.isLogged)
  const requireAuth = (): boolean => !!tokenStore.token

  // ─── 应用信息 ─────────────────────────────────────────────────
  const appInfo = ref<{ name: string; icon: string } | null>(null)

  const fetchAppInfo = async () => {
    if (!requireAuth()) return
    try {
      const res = await conversationAPI.getApplication(applicationId.value)
      appInfo.value = { name: res.data.name || '', icon: res.data.icon || '' }
    } catch {}
  }

  // ─── 会话 CRUD ─────────────────────────────────────────────────
  const {chats, hasMore, loadingMore, pageConversation, loadMore} = useConversationCrud({
    pageConversationAPI: (query: any, loading?: Ref<boolean>) => {
      return conversationAPI.pageConversation(applicationId.value, query, loading)
    }
  })

  const conversationId = computed(() => route.params.conversationId as string | undefined)
  const applicationId = computed(() => route.params.applicationId as string)

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
      return conversationAPI.pageConversationMessage(applicationId.value, cid, query)
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
        {type: 'label', label: t(GROUP_I18N_KEY[key])} as FlatLabel,
        ...items.map((c) => ({type: 'item', ...c}) as FlatItem)
      ]
    })
  })

  // ─── 路由导航 ─────────────────────────────────────────────────
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
    router.push({name: 'conversation-new'})
  }

  // ─── 会话操作 ─────────────────────────────────────────────────
  const newChat = async (name?: string) => {
    if (!requireAuth()) return undefined as any
    const res = await conversationAPI.createConversation(applicationId.value, name && name?.length > 0 ? name : t('conversation.newChat'))

    chats.value.unshift(res.data)

    await toConversation(res.data.id)

    messages.value = []
    resetMsgState(res.data.id)
    clearStoredStreamIndex(res.data.id)

    return res.data
  }

  const switchChat = (id: string) => toConversation(id)

  const deleteChat = async (id: string) => {
    if (!requireAuth()) return
    await conversationAPI.delConversation(applicationId.value, id)

    const idx = chats.value.findIndex((c) => c.id === id)
    if (idx < 0) return

    clearMsgState(id)
    clearStoredStreamIndex(id)

    chats.value.splice(idx, 1)

    if (!chats.value.length) {
      messages.value = []
      router.push({name: 'conversation-new'})
      return
    }

    if (conversationId.value === id) {
      const next = chats.value[idx] ?? chats.value[idx - 1]
      if (next?.id) await switchChat(next.id)
    }
  }

  const renameChat = (id: string, name: string) => {
    if (!requireAuth()) return
    const trimmed = name.trim()
    if (!trimmed) return

    conversationAPI.modifyName(applicationId.value, id, trimmed).then(() => {
      const c = chats.value.find((x) => x.id === id)
      if (c) c.name = trimmed
    })
  }

  const chat = (q: any) => {
    if (!requireAuth()) return Promise.reject(new Error('not logged in'))
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    return conversationAPI.conversation(applicationId.value, cid, q)
  }

  // ─── 流式操作 ─────────────────────────────────────────────────
  const statusStream = () => {
    if (!requireAuth()) return Promise.reject(new Error('not logged in'))
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    return conversationAPI.statusStream(applicationId.value, cid)
  }

  const resumeStream = () => {
    if (!requireAuth()) return Promise.reject(new Error('not logged in'))
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    const index = getStoredStreamIndex(cid)
    return conversationAPI.resumeStream(applicationId.value, cid, index)
  }
  const cancel = () => {
    if (!requireAuth()) return Promise.reject(new Error('not logged in'))
    const cid = conversationId.value
    if (!cid) throw new Error('conversationId is empty')
    return conversationAPI.cancel(applicationId.value, cid)
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
  const init = async (query?: QueryConversationVO, loading?: Ref<boolean>) => {
    if (!requireAuth()) return
    if (!query) {
      query = {currentPage: 1, pageSize: 30}
    }

    fetchAppInfo()
    await pageConversation(query, loading)
  }

  return {
    // 状态
    isLogged,
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
    loadMore: () => { if (requireAuth()) return loadMore() },
    loadMessages,
    loadMoreMessages: () => { if (requireAuth()) return loadMoreMessages(conversationId.value) },
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
    // stream manager（供 chat-panel 直接调用）
    startStream: streamManager.startStream,
    cancelStream: streamManager.cancelStream,
    switchConversation: (opts: {
      cid: string;
      getOnStream: () => (chunk: any) => void;
      onFinish?: () => void;
      onFailure?: () => void;
      skipLoadMessages?: boolean
    }) => {
      return streamManager.switchConversation({
        ...opts,
        loadMessages,
        statusStream,
        resumeStream
      })
    }
  }
}
