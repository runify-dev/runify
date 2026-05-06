import { ref, reactive } from 'vue'
import type { Msg } from '../../types'

const MSG_PAGE_SIZE = 10

// 单例 — 所有 store 实例共享同一个消息列表
const messages = ref<Msg[]>([])
const msgCurrentPageMap = reactive(new Map<string, number>())
const msgTotalMap = reactive(new Map<string, number>())
const msgLoadingMap = reactive(new Map<string, boolean>())

interface MessagePaginationDeps {
  pageConversationMessage: (cid: string, query: any) => Promise<any>
  getCurrentName?: () => string | undefined
  getCurrentCreateTime?: () => string | undefined
}

export function useMessagePagination(deps: MessagePaginationDeps) {
  const { pageConversationMessage, getCurrentName, getCurrentCreateTime } = deps

  const getMsgCurrentPage = (cid: string) => msgCurrentPageMap.get(cid) ?? 1
  const setMsgCurrentPage = (cid: string, page: number) => msgCurrentPageMap.set(cid, page)

  const getMsgTotal = (cid: string) => msgTotalMap.get(cid) ?? 0
  const setMsgTotal = (cid: string, total: number) => msgTotalMap.set(cid, total)

  const isMsgLoading = (cid: string) => msgLoadingMap.get(cid) === true
  const setMsgLoading = (cid: string, loading: boolean) => msgLoadingMap.set(cid, loading)

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

  const msgLoading = (cid: string | undefined) => cid ? isMsgLoading(cid) : false

  const hasMoreMsg = (cid: string | undefined) => {
    if (!cid) return false
    return messages.value.length < getMsgTotal(cid)
  }

  const loadMessages = async (cid: string, reset = true) => {
    if (isMsgLoading(cid)) return

    if (reset) {
      messages.value = []
      resetMsgState(cid)
    }

    setMsgLoading(cid, true)

    try {
      const res = await pageConversationMessage(cid, {
        currentPage: getMsgCurrentPage(cid),
        pageSize: MSG_PAGE_SIZE
      })

      setMsgTotal(cid, res.data.total)

      const records = [...res.data.records].reverse() as Msg[]

      if (reset) {
        messages.value = records
      } else {
        messages.value = [...records, ...messages.value]
      }
    } finally {
      setMsgLoading(cid, false)
    }
  }

  const loadMoreMessages = async (cid: string | undefined) => {
    if (!cid) return
    if (messages.value.length >= getMsgTotal(cid) || isMsgLoading(cid)) return

    const oldPage = getMsgCurrentPage(cid)
    setMsgCurrentPage(cid, oldPage + 1)

    try {
      await loadMessages(cid, false)
    } catch (e) {
      setMsgCurrentPage(cid, oldPage)
      throw e
    }
  }

  const pushMessage = (msg: Msg) => {
    messages.value.push(msg)

    if (messages.value.length === 1 && getCurrentName) {
      const question =
        (msg.content as any[]).find((item: any) => item.type === 'QUESTION')?.content ?? ''
      // 名称更新由 store 层处理（因为需要直接修改 current.value）
    }
  }

  return {
    messages,
    msgLoading,
    hasMoreMsg,
    loadMessages,
    loadMoreMessages,
    pushMessage,
    resetMsgState,
    clearMsgState
  }
}
